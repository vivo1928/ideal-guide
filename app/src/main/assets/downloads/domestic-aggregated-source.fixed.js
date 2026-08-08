/*!
 * @name 国内聚合音源 v3.1
 * @description 终极全并发版：所有音质×所有API同时请求，谁先返回用谁，极速获取链接
 * @version v3.1.0
 * @author 基于成熟方案整理
 */

const DEV_ENABLE = false

// --- 常量 ---
const CACHE_TTL_MS = 1800000  // 30分钟缓存
const CACHE_MAX_SIZE = 500
const HTTP_URL_REGEX = /^https?:\/\//i
// 官方CDN域名（优先选择这些域名的链接，速度更快）
const OFFICIAL_CDN_DOMAINS = [
  'isure.stream.qqmusic.qq.com',
  'aqqmusic.tc.qq.com',
  'dl.stream.qqmusic.qq.com',
  'streamoc.music.tc.qq.com',
  'isure2.stream.qqmusic.qq.com',
  'm7.music.126.net',
  'm8.music.126.net',
  'm10.music.126.net',
]
// 单个API请求超时时间（毫秒）
const REQUEST_TIMEOUT = 4000
// 整体等待超时（毫秒）- 有结果立即返回
const OVERALL_TIMEOUT = 3500

// --- API端点 ---
const XINGHAI_MAIN_API = "https://music-api.gdstudio.xyz/api.php?use_xbridge3=true&loader_name=forest&need_sec_link=1&sec_link_scene=im&theme=light"

// Meting API镜像列表
const METING_APIS = [
  "https://music.3e0.cn/",
  "https://api.injahow.cn/meting/",
  "https://meting.qjqq.cn/",
  "https://api.moeyao.cn/meting/",
  "https://meting-api.mcloc.cn/",
  "https://api.i-meto.com/meting/api/",
]

// 长青SVIP
const CHANGQING_URL_TEMPLATES = {
  tx: "http://175.27.166.236/kgqq/qq.php?type=mp3&id={id}&level={level}",
  wy: "http://175.27.166.236/wy/wy.php?type=mp3&id={id}&level={level}",
  kw: "https://musicapi.haitangw.net/music/kw.php?type=mp3&id={id}&level={level}",
  kg: "https://music.haitangw.cc/kgqq/kg.php?type=mp3&id={id}&level={level}",
  mg: "https://music.haitangw.cc/musicapi/mg.php?type=mp3&id={id}&level={level}"
}

// 念心SVIP
const NIANXIN_URL_TEMPLATES = {
  tx: "https://music.nxinxz.com/kgqq/tx.php?id={id}&level={level}&type=mp3",
  wy: "http://music.nxinxz.com/wy.php?id={id}&level={level}&type=mp3",
  kw: "http://music.nxinxz.com/kw.php?id={id}&level={level}&type=mp3",
  kg: "https://music.nxinxz.com/kgqq/kg.php?id={id}&level={level}&type=mp3",
  mg: "http://music.nxinxz.com/mg.php?id={id}&level={level}&type=mp3"
}

// 各平台支持音质
const PLATFORM_QUALITIES = {
  wy: ["flac", "320k", "192k", "128k"],
  tx: ["flac", "320k", "192k", "128k"],
  kw: ["flac", "320k", "192k", "128k"],
  kg: ["flac", "320k", "192k", "128k"],
  mg: ["flac", "320k", "192k", "128k"],
  xm: ["128k"]
}

// 音质优先级分数（越高越好）
const QUALITY_SCORE = {
  "flac": 1000,
  "320k": 800,
  "192k": 600,
  "128k": 400
}

const PLATFORM_TO_XINGHAI = {
  wy: "netease",
  kg: "kugou",
  kw: "kuwo",
  mg: "migu"
}

const METING_SERVER_MAP = {
  wy: "netease",
  tx: "tencent",
  kg: "kugou",
  kw: "kuwo",
  mg: "migu"
}

const QUALITY_TO_BR = {
  "128k": "128",
  "192k": "192",
  "320k": "320",
  "flac": "999"
}

const urlCache = new Map()

const { EVENT_NAMES, request, on, send } = globalThis.lx

// --- HTTP请求（自动跟随重定向）---
function httpRequest(url, options = { method: "GET" }) {
  return new Promise((resolve, reject) => {
    request(url, { timeout: REQUEST_TIMEOUT, follow_max: 5, ...options }, (err, res) => {
      if (err) return reject(new Error(`请求错误: ${err.message}`))
      let body = res?.body
      const finalUrl = res?.url || url
      const contentType = res?.headers?.['content-type'] || ''
      const isOfficialCdn = OFFICIAL_CDN_DOMAINS.some(domain => finalUrl.includes(domain))
      
      // 音频流
      if (contentType.includes('audio') || contentType.includes('octet-stream')) {
        return resolve({
          type: 'audio',
          url: finalUrl,
          isOfficial: isOfficialCdn
        })
      }
      
      if (typeof body === "string") {
        const trimmed = body.trim()
        // 直接返回URL字符串
        if (HTTP_URL_REGEX.test(trimmed) && trimmed.length > 25) {
          const urlIsOfficial = OFFICIAL_CDN_DOMAINS.some(domain => trimmed.includes(domain))
          return resolve({
            type: 'url',
            url: trimmed,
            isOfficial: urlIsOfficial || isOfficialCdn
          })
        }
        // JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
          try { body = JSON.parse(trimmed) } catch (e) {}
        }
        // JSONP
        const jsonMatch = trimmed.match(/^[^(]+\((.+)\)$/s)
        if (jsonMatch) {
          try { body = JSON.parse(jsonMatch[1]) } catch (e) {}
        }
      }
      resolve({
        type: 'json',
        data: body,
        finalUrl,
        isOfficialCdn
      })
    })
  })
}

// --- 辅助函数 ---
function qualityToLevel(quality) {
  const q = String(quality || "128k").toLowerCase()
  if (q === "flac") return "lossless"
  if (q === "320k") return "exhigh"
  if (q === "192k") return "high"
  return "standard"
}

function getSongId(songInfo) {
  return songInfo?.hash ?? songInfo?.songmid ?? songInfo?.id ?? null
}

function validateUrl(url) {
  if (!url || typeof url !== "string") throw new Error("空URL")
  const trimmed = url.trim()
  if (!HTTP_URL_REGEX.test(trimmed)) throw new Error("非法URL")
  if (trimmed.length < 25) throw new Error("URL太短")
  return trimmed
}

// 计算URL综合得分：音质分 + CDN分
function calcUrlScore(url, quality) {
  let score = QUALITY_SCORE[quality] || 0
  // 官方CDN加分
  for (let i = 0; i < OFFICIAL_CDN_DOMAINS.length; i++) {
    if (url.includes(OFFICIAL_CDN_DOMAINS[i])) {
      score += 500 - i * 10
      break
    }
  }
  // 文件格式加分
  if (url.includes('.flac?') || url.includes('.flac')) score += 50
  if (url.includes('.mp3?') || url.includes('.mp3') || url.includes('.m4a')) score += 20
  return score
}

// 缓存
function buildCacheKey(songInfo, quality) {
  const name = songInfo?.name || ""
  const singer = songInfo?.singer || ""
  const id = getSongId(songInfo) || ""
  return `url_${id}_${name}_${singer}_${quality}`
}

function getCachedUrl(songInfo) {
  // 尝试所有音质缓存
  const qualities = ["flac", "320k", "192k", "128k"]
  for (const q of qualities) {
    const key = buildCacheKey(songInfo, q)
    const entry = urlCache.get(key)
    if (entry && Date.now() - entry.timestamp < CACHE_TTL_MS) {
      return entry.url
    }
  }
  return null
}

function setCachedUrl(songInfo, quality, url) {
  // 清理过期缓存
  for (const q of ["flac", "320k", "192k", "128k"]) {
    const key = buildCacheKey(songInfo, q)
    const entry = urlCache.get(key)
    if (entry && Date.now() - entry.timestamp >= CACHE_TTL_MS) {
      urlCache.delete(key)
    }
  }
  const key = buildCacheKey(songInfo, quality)
  urlCache.set(key, { url, timestamp: Date.now() })
  if (urlCache.size > CACHE_MAX_SIZE) {
    const oldestKey = urlCache.keys().next().value
    if (oldestKey !== undefined) urlCache.delete(oldestKey)
  }
}

// --- 终极并发：所有请求同时发出，选综合得分最高的 ---
function promiseAllConcurrent(promises, timeoutMs = OVERALL_TIMEOUT) {
  return new Promise((resolve, reject) => {
    let pending = promises.length
    let resolved = false
    let bestResult = null
    let bestScore = -1
    let errors = []
    
    if (pending === 0) return reject(new Error("无可用请求"))
    
    const timer = setTimeout(() => {
      if (!resolved) {
        if (bestResult) {
          resolved = true
          resolve(bestResult)
        } else {
          resolved = true
          reject(new Error(`超时(${timeoutMs}ms): ${errors.slice(0,3).join("; ")}`))
        }
      }
    }, timeoutMs)
    
    const handleResult = (url, quality, apiName) => {
      if (resolved) return
      try {
        const validUrl = validateUrl(url)
        const score = calcUrlScore(validUrl, quality)
        
        if (DEV_ENABLE) console.log(`  ✓ ${apiName}(${quality}) 得分:${score}`)
        
        // 极高分（无损+官方CDN）直接返回
        if (score >= 1400) {
          resolved = true
          clearTimeout(timer)
          resolve({ url: validUrl, quality })
          return
        }
        
        // 否则记录最佳
        if (score > bestScore) {
          bestScore = score
          bestResult = { url: validUrl, quality }
        }
        
        // 分数够高也可以直接返回了（不用等全部）
        if (bestScore >= 1200) {
          resolved = true
          clearTimeout(timer)
          resolve(bestResult)
          return
        }
      } catch (e) {
        // 验证失败，忽略
      }
      pending--
      checkDone()
    }
    
    const handleError = (err) => {
      errors.push(err?.message || "失败")
      pending--
      checkDone()
    }
    
    const checkDone = () => {
      if (resolved) return
      if (pending === 0) {
        if (bestResult) {
          resolved = true
          clearTimeout(timer)
          resolve(bestResult)
        } else {
          clearTimeout(timer)
          reject(new Error(errors.slice(0,3).join("; ")))
        }
      }
    }
    
    promises.forEach(p => {
      p.then(r => handleResult(r.url, r.quality, r.apiName))
       .catch(e => handleError(e))
    })
  })
}

// --- 单个API请求（包装结果包含音质信息）---

async function reqXinghai(platform, songId, quality) {
  const source = PLATFORM_TO_XINGHAI[platform]
  if (!source) throw new Error("星海不支持")
  const br = QUALITY_TO_BR[quality] || "128"
  const url = `${XINGHAI_MAIN_API}&types=url&source=${encodeURIComponent(source)}&id=${encodeURIComponent(songId)}&br=${br}`
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url, quality, apiName: "星海" }
  if (res.data?.url) return { url: res.data.url, quality, apiName: "星海" }
  throw new Error("无URL")
}

async function reqChangqing(platform, songId, quality) {
  const template = CHANGQING_URL_TEMPLATES[platform]
  if (!template) throw new Error("长青不支持")
  const level = qualityToLevel(quality)
  const url = template.replace("{id}", encodeURIComponent(String(songId))).replace("{level}", encodeURIComponent(level))
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url, quality, apiName: "长青" }
  if (res.data?.url) return { url: res.data.url, quality, apiName: "长青" }
  if (res.data?.data?.url) return { url: res.data.data.url, quality, apiName: "长青" }
  throw new Error("无URL")
}

async function reqNianxin(platform, songId, quality) {
  const template = NIANXIN_URL_TEMPLATES[platform]
  if (!template) throw new Error("念心不支持")
  const level = qualityToLevel(quality)
  const url = template.replace("{id}", encodeURIComponent(String(songId))).replace("{level}", encodeURIComponent(level))
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url, quality, apiName: "念心" }
  if (res.data?.url) return { url: res.data.url, quality, apiName: "念心" }
  if (res.data?.data?.url) return { url: res.data.data.url, quality, apiName: "念心" }
  throw new Error("无URL")
}

async function reqMeting(apiBase, platform, songId, quality) {
  const server = METING_SERVER_MAP[platform]
  if (!server) throw new Error("Meting不支持")
  const br = QUALITY_TO_BR[quality] || "128"
  const url = `${apiBase}?type=url&server=${server}&id=${encodeURIComponent(songId)}&br=${br}`
  const res = await httpRequest(url)
  const apiName = "Meting"
  if (res.type === 'audio' || res.type === 'url') return { url: res.url, quality, apiName }
  if (res.data?.url) return { url: res.data.url, quality, apiName }
  if (Array.isArray(res.data) && res.data[0]?.url) return { url: res.data[0].url, quality, apiName }
  throw new Error("无URL")
}

// --- 喜马拉雅多源解析 ---
const XM_AES_KEY_HEX = "aaad3e4fd540b0f79dca95606e72bf93"
const XM_TRACK_V1_API = "https://mobile.ximalaya.com/mobile/v1/track/baseInfo"
const XM_TRACK_V3_API = "https://www.ximalaya.com/mobile-playpage/track/v3/baseInfo"
const XM_TELECOM_API = "https://api.telecom.ac.cn/ximalaya"
const XM_REVISION_PLAY_API = "https://www.ximalaya.com/revision/play/v1/audio"
const XM_REVISION_TRACKS_API = "https://www.ximalaya.com/revision/play/tracks"

function xmIsPreviewUrl(url) {
  if (!url) return true
  return url.includes("preview_") || url.includes("duration=180") || url.includes("sample")
}

function xmAesEcbDecrypt(ciphertext) {
  try {
    let crypto
    if (typeof require !== "undefined") {
      crypto = require("crypto")
    } else {
      return null
    }
    let base64 = ciphertext.replace(/-/g, "+").replace(/_/g, "/")
    while (base64.length % 4) base64 += "="
    const encrypted = Buffer.from(base64, "base64")
    const key = Buffer.from(XM_AES_KEY_HEX, "hex")
    const decipher = crypto.createDecipheriv("aes-128-ecb", key, null)
    decipher.setAutoPadding(false)
    let decrypted = decipher.update(encrypted)
    decrypted = Buffer.concat([decrypted, decipher.final()])
    return decrypted.toString("utf-8").replace(/[^\x20-\x7E]/g, "")
  } catch (e) {
    return null
  }
}

async function xmParseOfficialV1(trackId) {
  const url = `${XM_TRACK_V1_API}?trackId=${encodeURIComponent(trackId)}&device=pc`
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url }
  if (res.data && typeof res.data === 'object') {
    let playUrl = res.data.playUrl64 || res.data.play_path_64 || ""
    if (!playUrl) playUrl = res.data.playUrl32 || res.data.play_path_32 || ""
    if (!playUrl) playUrl = res.data.playPathAacv224 || res.data.play_path_aacv224 || ""
    if (!playUrl) playUrl = res.data.playUrl || res.data.play_path || ""
    if (!playUrl) playUrl = res.data.downloadUrl || ""
    if (playUrl) return { url: playUrl }
  }
  throw new Error("V1无链接")
}

async function xmParseOfficialV3(trackId) {
  const ts = Date.now()
  const url = `${XM_TRACK_V3_API}/${ts}?device=web&trackId=${encodeURIComponent(trackId)}&trackQualityLevel=3`
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url }
  if (res.data && typeof res.data === 'object') {
    const trackInfo = res.data.trackInfo
    if (trackInfo && trackInfo.playUrlList && Array.isArray(trackInfo.playUrlList)) {
      const sorted = trackInfo.playUrlList
        .filter(item => item && item.url && typeof item.url === 'string')
        .sort((a, b) => (b.fileSize || 0) - (a.fileSize || 0))
      for (const item of sorted) {
        const decrypted = xmAesEcbDecrypt(item.url)
        if (decrypted && decrypted.startsWith("http")) return { url: decrypted }
      }
    }
  }
  throw new Error("V3无链接")
}

async function xmParseTelecom(trackId) {
  const quaList = [1, 0, 3, 2, 4]
  for (const qua of quaList) {
    try {
      const url = `${XM_TELECOM_API}?all=0&trackid=${encodeURIComponent(trackId)}&qua=${qua}`
      const res = await httpRequest(url)
      if (res.type === 'audio' || res.type === 'url') return { url: res.url }
      if (res.data && typeof res.data === 'object' && res.data.Status === 'True') {
        const audioUrls = res.data.AudioUrls
        if (audioUrls && Array.isArray(audioUrls) && audioUrls.length > 0) {
          const first = audioUrls[0]
          if (first && first.url && first.url.startsWith("http")) return { url: first.url }
        }
      }
    } catch (e) { continue }
  }
  throw new Error("Telecom无链接")
}

async function xmParseRevisionPlayV1(trackId) {
  const url = `${XM_REVISION_PLAY_API}?id=${encodeURIComponent(trackId)}&ptype=1`
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url }
  if (res.data && typeof res.data === 'object') {
    const data = res.data
    if (data.ret === 200) {
      const src = data.src || data.data?.src || ""
      if (src && src.startsWith("http")) return { url: src }
    }
  }
  throw new Error("RevisionPlay无链接")
}

async function xmParseRevisionTracks(trackId) {
  const url = `${XM_REVISION_TRACKS_API}?trackIds=${encodeURIComponent(trackId)}`
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url }
  if (res.data && typeof res.data === 'object') {
    const data = res.data
    if (data.ret === 200) {
      const tracks = data.tracks || data.data?.tracks || []
      if (Array.isArray(tracks) && tracks.length > 0) {
        const src = tracks[0].src || tracks[0].playUrl || tracks[0].play_path || ""
        if (src && src.startsWith("http")) return { url: src }
      }
    }
  }
  throw new Error("RevisionTracks无链接")
}

async function xmParseMPlayPage(trackId) {
  const url = `https://m.ximalaya.com/m-revision/page/track/queryTrackPage/${encodeURIComponent(trackId)}`
  const res = await httpRequest(url)
  if (res.type === 'audio' || res.type === 'url') return { url: res.url }
  if (res.data && typeof res.data === 'object') {
    const trackInfo = res.data.trackInfo || res.data.data?.trackInfo || {}
    if (trackInfo.playUrlList && Array.isArray(trackInfo.playUrlList)) {
      const sorted = trackInfo.playUrlList
        .filter(item => item && item.url && typeof item.url === 'string')
        .sort((a, b) => (b.fileSize || 0) - (a.fileSize || 0))
      for (const item of sorted) {
        const decrypted = xmAesEcbDecrypt(item.url)
        if (decrypted && decrypted.startsWith("http")) return { url: decrypted }
      }
    }
    const src = trackInfo.src || trackInfo.playUrl || ""
    if (src && src.startsWith("http")) return { url: src }
  }
  throw new Error("MPlayPage无链接")
}

// 喜马拉雅：多源解析，免费内容获取完整版，付费内容回退到预览版
async function reqXimalaya(trackId, quality) {
  const apiName = "喜马拉雅"
  let previewUrl = null
  let errors = []

  // 1. Revision Play V1 API（网页播放接口，无加密）
  try {
    const result = await xmParseRevisionPlayV1(trackId)
    if (!xmIsPreviewUrl(result.url)) return { url: result.url, quality, apiName }
    previewUrl = result.url
  } catch (e) { errors.push(e.message) }

  // 2. Revision Tracks API（批量音轨接口）
  try {
    const result = await xmParseRevisionTracks(trackId)
    if (!xmIsPreviewUrl(result.url)) return { url: result.url, quality, apiName }
    if (!previewUrl) previewUrl = result.url
  } catch (e) { errors.push(e.message) }

  // 3. M站移动播放页API
  try {
    const result = await xmParseMPlayPage(trackId)
    if (!xmIsPreviewUrl(result.url)) return { url: result.url, quality, apiName }
    if (!previewUrl) previewUrl = result.url
  } catch (e) { errors.push(e.message) }

  // 4. V1 API
  try {
    const result = await xmParseOfficialV1(trackId)
    if (!xmIsPreviewUrl(result.url)) return { url: result.url, quality, apiName }
    if (!previewUrl) previewUrl = result.url
  } catch (e) { errors.push(e.message) }

  // 5. V3 API（AES解密）
  try {
    const result = await xmParseOfficialV3(trackId)
    if (!xmIsPreviewUrl(result.url)) return { url: result.url, quality, apiName }
    if (!previewUrl) previewUrl = result.url
  } catch (e) { errors.push(e.message) }

  // 6. Telecom API
  try {
    const result = await xmParseTelecom(trackId)
    if (!xmIsPreviewUrl(result.url)) return { url: result.url, quality, apiName }
    if (!previewUrl) previewUrl = result.url
  } catch (e) { errors.push(e.message) }

  // 7. 预览版后备
  if (previewUrl) return { url: previewUrl, quality, apiName }

  throw new Error(`喜马拉雅无播放链接 (${errors.join("; ")})`)
}

// --- 主获取逻辑：所有音质×所有API 全部同时并发！---
async function getUrlWithFallback(platform, songInfo, quality) {
  if (!platform || !PLATFORM_QUALITIES[platform]) throw new Error("无效平台")
  if (!songInfo || typeof songInfo !== "object") throw new Error("无效歌曲信息")

  const songId = getSongId(songInfo)
  if (!songId) throw new Error("缺少歌曲ID")

  // 喜马拉雅独立处理
  if (platform === "xm") {
    return reqXimalaya(songId, quality || "128k").then(r => r.url)
  }

  // 检查缓存（任意音质缓存都可以用）
  const cached = getCachedUrl(songInfo)
  if (cached) {
    if (DEV_ENABLE) console.log("使用缓存")
    return cached
  }

  // 确定要尝试的音质列表（从用户请求的音质开始，包含所有更低音质）
  const allQualities = ["flac", "320k", "192k", "128k"]
  const requestedQuality = quality || "128k"
  const startIdx = allQualities.indexOf(requestedQuality)
  const qualitiesToTry = startIdx >= 0 
    ? allQualities.slice(startIdx) 
    : [requestedQuality, ...allQualities.filter(q => q !== requestedQuality)]

  if (DEV_ENABLE) {
    console.log("=== v3.1终极全并发 ===")
    console.log("平台:", platform, "歌曲:", songInfo.name, "-", songInfo.singer)
    console.log("尝试音质:", qualitiesToTry.join(", "))
  }

  // ========== 关键：构建所有请求，不管音质不管API，全部同时发出！==========
  const allPromises = []
  
  for (const q of qualitiesToTry) {
    if (!PLATFORM_QUALITIES[platform].includes(q)) continue
    
    // 该音质的所有Meting镜像
    for (const apiBase of METING_APIS) {
      allPromises.push(reqMeting(apiBase, platform, songId, q))
    }
    
    // 星海（如果支持）
    if (PLATFORM_TO_XINGHAI[platform]) {
      allPromises.push(reqXinghai(platform, songId, q))
    }
    
    // 长青
    allPromises.push(reqChangqing(platform, songId, q))
    
    // 念心
    allPromises.push(reqNianxin(platform, songId, q))
  }
  
  if (DEV_ENABLE) console.log(`总并发请求数: ${allPromises.length}`)

  // 所有请求同时发出，等待最快/最好的结果
  const result = await promiseAllConcurrent(allPromises, OVERALL_TIMEOUT)
  const validUrl = validateUrl(result.url)
  
  // 缓存结果
  setCachedUrl(songInfo, result.quality, validUrl)
  
  if (DEV_ENABLE) console.log(`最终结果: ${result.apiName}(${result.quality}), 得分:${calcUrlScore(validUrl, result.quality)}`)
  
  return validUrl
}

// --- 注册音源 ---
const sourceConfig = {}
const PLATFORM_NAMES = {
  wy: "网易云音乐", tx: "QQ音乐", kw: "酷我音乐", kg: "酷狗音乐", mg: "咪咕音乐", xm: "喜马拉雅"
}
Object.keys(PLATFORM_QUALITIES).forEach(platform => {
  sourceConfig[platform] = {
    name: PLATFORM_NAMES[platform],
    type: "music",
    actions: ["musicUrl"],
    qualitys: PLATFORM_QUALITIES[platform]
  }
})

on(EVENT_NAMES.request, ({ action, source, info }) => {
  if (action !== "musicUrl") return Promise.reject(new Error("不支持"))
  if (!info?.musicInfo) return Promise.reject(new Error("参数不完整"))
  return getUrlWithFallback(source, info.musicInfo, info.type || "128k")
    .then(url => Promise.resolve(url))
    .catch(err => Promise.reject(err))
})

send(EVENT_NAMES.inited, {
  status: true,
  openDevTools: DEV_ENABLE,
  sources: sourceConfig
})
