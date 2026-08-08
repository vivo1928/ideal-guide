/**
 * @name 喜马拉雅音源
 * @description 喜马拉雅FM有声书/音频专属音源：支持搜索与播放链接获取，免费内容直接播放，付费内容自动回退到预览版
 * @version v2.0.0
 * @author 基于国内聚合音源改编
 */

const DEV_ENABLE = false

const { EVENT_NAMES, request, on, send } = globalThis.lx

// --- 常量 ---
const CACHE_TTL_MS = 1800000
const CACHE_MAX_SIZE = 500
const REQUEST_TIMEOUT = 8000

// --- API端点 ---
const XM_SEARCH_API = "https://www.ximalaya.com/revision/search"
const XM_TRACK_V1_API = "https://mobile.ximalaya.com/mobile/v1/track/baseInfo"
const XM_TRACK_V3_API = "https://www.ximalaya.com/mobile-playpage/track/v3/baseInfo"
const XM_TELECOM_API = "https://api.telecom.ac.cn/ximalaya"
const XM_REVISION_PLAY_API = "https://www.ximalaya.com/revision/play/v1/audio"
const XM_REVISION_TRACKS_API = "https://www.ximalaya.com/revision/play/tracks"
const XM_M_PAY_API = "https://mpay.ximalaya.com/mobile/track/pay"

const HTTP_URL_REGEX = /^https?:\/\//i

const urlCache = new Map()

// --- AES ECB 解密（用于官方v3 API）---
const AES_KEY_HEX = "aaad3e4fd540b0f79dca95606e72bf93"

function aesEcbDecrypt(ciphertext) {
  try {
    let crypto
    if (typeof require !== "undefined") {
      crypto = require("crypto")
    } else {
      return null
    }
    // base64 urlsafe -> standard
    let base64 = ciphertext.replace(/-/g, "+").replace(/_/g, "/")
    while (base64.length % 4) base64 += "="
    const encrypted = Buffer.from(base64, "base64")
    const key = Buffer.from(AES_KEY_HEX, "hex")
    const decipher = crypto.createDecipheriv("aes-128-ecb", key, null)
    decipher.setAutoPadding(false)
    let decrypted = decipher.update(encrypted)
    decrypted = Buffer.concat([decrypted, decipher.final()])
    // 去掉不可打印字符
    return decrypted.toString("utf-8").replace(/[^\x20-\x7E]/g, "")
  } catch (e) {
    if (DEV_ENABLE) console.log("[AES解密失败]", e.message)
    return null
  }
}

// --- HTTP请求 ---
function httpRequest(url, options = { method: "GET" }) {
  return new Promise((resolve, reject) => {
    request(url, { timeout: REQUEST_TIMEOUT, follow_max: 5, ...options }, (err, res) => {
      if (err) return reject(new Error(`请求错误: ${err.message}`))
      let body = res?.body
      const finalUrl = res?.url || url
      const contentType = res?.headers?.["content-type"] || ""

      if (contentType.includes("audio") || contentType.includes("octet-stream")) {
        return resolve({ type: "audio", url: finalUrl })
      }

      if (typeof body === "string") {
        const trimmed = body.trim()
        if (HTTP_URL_REGEX.test(trimmed) && trimmed.length > 25) {
          return resolve({ type: "url", url: trimmed })
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
          try { body = JSON.parse(trimmed) } catch (e) {}
        }
      }
      resolve({ type: "json", data: body, finalUrl })
    })
  })
}

// --- 缓存 ---
function buildCacheKey(trackId) {
  return `xm_${trackId}`
}

function getCachedUrl(trackId) {
  const key = buildCacheKey(trackId)
  const entry = urlCache.get(key)
  if (entry && Date.now() - entry.timestamp < CACHE_TTL_MS) {
    return entry
  }
  if (entry) urlCache.delete(key)
  return null
}

function setCachedUrl(trackId, url, isPreview) {
  const key = buildCacheKey(trackId)
  urlCache.set(key, { url, isPreview: !!isPreview, timestamp: Date.now() })
  if (urlCache.size > CACHE_MAX_SIZE) {
    const oldestKey = urlCache.keys().next().value
    if (oldestKey !== undefined) urlCache.delete(oldestKey)
  }
}

// --- 判断是否为预览链接 ---
function isPreviewUrl(url) {
  if (!url) return true
  return url.includes("preview_") || url.includes("duration=180") || url.includes("sample")
}

// --- 官方V1 API（免费内容直接获取完整链接）---
async function parseOfficialV1(trackId) {
  const url = `${XM_TRACK_V1_API}?trackId=${encodeURIComponent(trackId)}&device=pc`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("V1 API返回格式异常")
  }
  const data = res.data
  // 付费内容不返回playUrl字段
  let playUrl = data.playUrl64 || data.play_path_64 || ""
  if (!playUrl) playUrl = data.playUrl32 || data.play_path_32 || ""
  if (!playUrl) playUrl = data.playPathAacv224 || data.play_path_aacv224 || ""
  if (!playUrl) playUrl = data.playUrl || data.play_path || ""
  if (!playUrl) playUrl = data.downloadUrl || ""
  if (!playUrl) throw new Error("V1 API无播放链接")
  return { url: playUrl, isPreview: isPreviewUrl(playUrl) }
}

// --- 官方V3 API（AES解密，适用于所有内容，付费为预览版）---
async function parseOfficialV3(trackId) {
  const ts = Date.now()
  const url = `${XM_TRACK_V3_API}/${ts}?device=web&trackId=${encodeURIComponent(trackId)}&trackQualityLevel=3`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("V3 API返回格式异常")
  }
  const trackInfo = res.data.trackInfo
  if (!trackInfo || !trackInfo.playUrlList || !Array.isArray(trackInfo.playUrlList)) {
    throw new Error("V3 API无playUrlList")
  }
  // 按fileSize从大到小排序，优先高质量
  const sorted = trackInfo.playUrlList
    .filter(item => item && item.url && typeof item.url === "string")
    .sort((a, b) => (b.fileSize || 0) - (a.fileSize || 0))

  for (const item of sorted) {
    const decrypted = aesEcbDecrypt(item.url)
    if (decrypted && decrypted.startsWith("http")) {
      return { url: decrypted, isPreview: isPreviewUrl(decrypted) }
    }
  }
  throw new Error("V3 API解密失败")
}

// --- Telecom API（第三方解析）---
async function parseTelecom(trackId) {
  // 尝试多个音质
  const quaList = [1, 0, 3, 2, 4]
  for (const qua of quaList) {
    try {
      const url = `${XM_TELECOM_API}?all=0&trackid=${encodeURIComponent(trackId)}&qua=${qua}`
      const res = await httpRequest(url)
      if (res.type !== "json" || !res.data || typeof res.data !== "object") continue
      if (res.data.Status !== "True") continue
      const audioUrls = res.data.AudioUrls
      if (!audioUrls || !Array.isArray(audioUrls) || audioUrls.length === 0) continue
      const first = audioUrls[0]
      if (first && first.url && first.url.startsWith("http")) {
        return { url: first.url, isPreview: isPreviewUrl(first.url) }
      }
    } catch (e) {
      continue
    }
  }
  throw new Error("Telecom API无播放链接")
}

// --- 官方Revision Play V1 API（网页播放接口，无加密）---
async function parseRevisionPlayV1(trackId) {
  const url = `${XM_REVISION_PLAY_API}?id=${encodeURIComponent(trackId)}&ptype=1`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("RevisionPlay V1返回格式异常")
  }
  const data = res.data
  if (data.ret !== 200) {
    throw new Error(data.msg || "RevisionPlay V1请求失败")
  }
  const src = data.src || data.data?.src || ""
  if (!src || !src.startsWith("http")) {
    throw new Error("RevisionPlay V1无src链接")
  }
  return { url: src, isPreview: isPreviewUrl(src) }
}

// --- 官方Revision Tracks API（批量音轨接口）---
async function parseRevisionTracks(trackId) {
  const url = `${XM_REVISION_TRACKS_API}?trackIds=${encodeURIComponent(trackId)}`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("RevisionTracks返回格式异常")
  }
  const data = res.data
  if (data.ret !== 200) {
    throw new Error(data.msg || "RevisionTracks请求失败")
  }
  const tracks = data.tracks || data.data?.tracks || []
  if (!Array.isArray(tracks) || tracks.length === 0) {
    throw new Error("RevisionTracks无音轨数据")
  }
  const track = tracks[0]
  const src = track.src || track.playUrl || track.play_path || ""
  if (!src || !src.startsWith("http")) {
    throw new Error("RevisionTracks无播放链接")
  }
  return { url: src, isPreview: isPreviewUrl(src) }
}

// --- M站移动播放页API（M站专用接口）---
async function parseMPlayPage(trackId) {
  const url = `https://m.ximalaya.com/m-revision/page/track/queryTrackPage/${encodeURIComponent(trackId)}`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("MPlayPage返回格式异常")
  }
  const trackInfo = res.data.trackInfo || res.data.data?.trackInfo || {}
  if (trackInfo.playUrlList && Array.isArray(trackInfo.playUrlList)) {
    const sorted = trackInfo.playUrlList
      .filter(item => item && item.url && typeof item.url === "string")
      .sort((a, b) => (b.fileSize || 0) - (a.fileSize || 0))
    for (const item of sorted) {
      const decrypted = aesEcbDecrypt(item.url)
      if (decrypted && decrypted.startsWith("http")) {
        return { url: decrypted, isPreview: isPreviewUrl(decrypted) }
      }
    }
  }
  const src = trackInfo.src || trackInfo.playUrl || ""
  if (src && src.startsWith("http")) {
    return { url: src, isPreview: isPreviewUrl(src) }
  }
  throw new Error("MPlayPage无播放链接")
}

// --- 喜马拉雅搜索 ---
async function searchXimalaya(keyword, page = 1, limit = 20) {
  const url = `${XM_SEARCH_API}?core=track&kw=${encodeURIComponent(keyword)}&page=${page}&spellchecker=true&rows=${limit}`
  const res = await httpRequest(url)

  if (res.type !== "json" || !res.data) {
    throw new Error("搜索返回格式异常")
  }

  const data = res.data
  if (data.ret !== 200) {
    throw new Error(data.msg || "搜索失败")
  }

  const docs = data.data?.result?.response?.docs || []
  const results = []

  for (const item of docs) {
    const trackId = String(item.id || "")
    if (!trackId) continue

    // 优先获取播放链接（搜索时直接带）
    let playUrl = item.play_path_64 || item.play_path_aacv224 || item.play_path_32 || ""

    results.push({
      id: trackId,
      name: item.title || "未知标题",
      singer: item.nickname || "未知主播",
      album: item.album_title || "",
      duration: item.duration || 0,
      cover: item.album_cover_path || item.cover_path || "",
      playUrl: playUrl,
      source: "xm"
    })
  }

  return results
}

// --- 喜马拉雅播放链接获取（多源解析）---
async function getXimalayaUrl(trackId) {
  // 检查缓存
  const cached = getCachedUrl(trackId)
  if (cached) return cached

  let previewUrl = null
  let errors = []

  // 1. 官方Revision Play V1 API（网页播放接口，无加密，免费内容直接返回完整链接）
  try {
    const result = await parseRevisionPlayV1(trackId)
    if (!result.isPreview) {
      setCachedUrl(trackId, result.url, false)
      return { url: result.url, isPreview: false }
    }
    previewUrl = result.url
  } catch (e) {
    errors.push(`RevPlay:${e.message}`)
  }

  // 2. 官方Revision Tracks API（批量音轨接口）
  try {
    const result = await parseRevisionTracks(trackId)
    if (!result.isPreview) {
      setCachedUrl(trackId, result.url, false)
      return { url: result.url, isPreview: false }
    }
    if (!previewUrl) previewUrl = result.url
  } catch (e) {
    errors.push(`RevTracks:${e.message}`)
  }

  // 3. M站移动播放页API
  try {
    const result = await parseMPlayPage(trackId)
    if (!result.isPreview) {
      setCachedUrl(trackId, result.url, false)
      return { url: result.url, isPreview: false }
    }
    if (!previewUrl) previewUrl = result.url
  } catch (e) {
    errors.push(`MPlay:${e.message}`)
  }

  // 4. 官方V1 API
  try {
    const result = await parseOfficialV1(trackId)
    if (!result.isPreview) {
      setCachedUrl(trackId, result.url, false)
      return { url: result.url, isPreview: false }
    }
    if (!previewUrl) previewUrl = result.url
  } catch (e) {
    errors.push(`V1:${e.message}`)
  }

  // 5. 官方V3 API（AES解密）
  try {
    const result = await parseOfficialV3(trackId)
    if (!result.isPreview) {
      setCachedUrl(trackId, result.url, false)
      return { url: result.url, isPreview: false }
    }
    if (!previewUrl) previewUrl = result.url
  } catch (e) {
    errors.push(`V3:${e.message}`)
  }

  // 6. Telecom API（第三方解析）
  try {
    const result = await parseTelecom(trackId)
    if (!result.isPreview) {
      setCachedUrl(trackId, result.url, false)
      return { url: result.url, isPreview: false }
    }
    if (!previewUrl) previewUrl = result.url
  } catch (e) {
    errors.push(`Telecom:${e.message}`)
  }

  // 7. 如果有预览链接，返回预览版（至少能播放180秒）
  if (previewUrl) {
    setCachedUrl(trackId, previewUrl, true)
    return { url: previewUrl, isPreview: true }
  }

  throw new Error(`喜马拉雅无播放链接 (${errors.join("; ")})`)
}

// --- 主处理逻辑 ---
on(EVENT_NAMES.request, ({ action, source, info }) => {
  if (action === "musicUrl") {
    if (!info?.musicInfo) return Promise.reject(new Error("参数不完整"))

    const musicInfo = info.musicInfo
    const trackId = musicInfo.id || musicInfo.songmid || musicInfo.hash || ""

    if (!trackId) return Promise.reject(new Error("缺少音频ID"))

    if (DEV_ENABLE) console.log("[喜马拉雅音源] 获取链接:", trackId, musicInfo.name)

    return getXimalayaUrl(trackId)
      .then(result => {
        if (DEV_ENABLE) console.log("[喜马拉雅音源] 结果:", result.url.substring(0, 80), "预览:", result.isPreview)
        return Promise.resolve(result.url)
      })
      .catch(err => Promise.reject(err))
  }

  if (action === "pic") {
    if (!info?.musicInfo) return Promise.reject(new Error("参数不完整"))
    const coverUrl = info.musicInfo.cover || info.musicInfo.pic || ""
    if (coverUrl) return Promise.resolve(coverUrl)
    return Promise.reject(new Error("无封面"))
  }

  return Promise.reject(new Error(`不支持的操作: ${action}`))
})

// --- 注册音源 ---
const sourceConfig = {
  xm: {
    name: "喜马拉雅",
    type: "music",
    actions: ["musicUrl", "pic"],
    qualitys: ["128k"]
  }
}

send(EVENT_NAMES.inited, {
  status: true,
  openDevTools: DEV_ENABLE,
  sources: sourceConfig
})

if (DEV_ENABLE) console.log("[喜马拉雅音源] 初始化完成 v2.0")
