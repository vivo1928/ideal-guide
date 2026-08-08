/**
 * @name 哔哩哔哩音源
 * @description 哔哩哔哩视频/音频专属音源：支持视频和音频搜索与播放链接获取
 * @version v1.0.0
 * @author 基于国内聚合音源改编
 */

const DEV_ENABLE = false
const { EVENT_NAMES, request, on, send } = globalThis.lx

// --- 常量 ---
const CACHE_TTL_MS = 1800000
const CACHE_MAX_SIZE = 500
const REQUEST_TIMEOUT = 8000

const HTTP_URL_REGEX = /^https?:\/\//i

const urlCache = new Map()

// --- HTTP请求 ---
function httpRequest(url, options = { method: "GET" }) {
  return new Promise((resolve, reject) => {
    request(url, { timeout: REQUEST_TIMEOUT, follow_max: 5, ...options }, (err, res) => {
      if (err) return reject(new Error(`请求错误: ${err.message}`))
      let body = res?.body
      const finalUrl = res?.url || url
      const contentType = res?.headers?.["content-type"] || ""

      if (contentType.includes("audio") || contentType.includes("octet-stream") || contentType.includes("video")) {
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
function buildCacheKey(id, type) {
  return `bl_${type}_${id}`
}

function getCachedUrl(id, type) {
  const key = buildCacheKey(id, type)
  const entry = urlCache.get(key)
  if (entry && Date.now() - entry.timestamp < CACHE_TTL_MS) {
    return entry.url
  }
  if (entry) urlCache.delete(key)
  return null
}

function setCachedUrl(id, type, url) {
  const key = buildCacheKey(id, type)
  urlCache.set(key, { url, timestamp: Date.now() })
  if (urlCache.size > CACHE_MAX_SIZE) {
    const oldestKey = urlCache.keys().next().value
    if (oldestKey !== undefined) urlCache.delete(oldestKey)
  }
}

// --- 获取视频CID ---
async function getVideoCid(bvid) {
  const url = `https://api.bilibili.com/x/web-interface/view?bvid=${encodeURIComponent(bvid)}`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("视频信息返回格式异常")
  }
  const data = res.data
  if (data.code !== 0) {
    throw new Error(data.message || "获取视频信息失败")
  }
  const cid = data.data?.cid
  if (!cid) throw new Error("无法获取视频CID")
  return String(cid)
}

// --- 获取视频播放链接 ---
async function getVideoUrl(bvid) {
  const cached = getCachedUrl(bvid, "video")
  if (cached) return cached

  const cid = await getVideoCid(bvid)
  const url = `https://api.bilibili.com/x/player/playurl?bvid=${encodeURIComponent(bvid)}&cid=${encodeURIComponent(cid)}&qn=80&fnver=0&fnval=16&fourk=1`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("播放链接返回格式异常")
  }
  const data = res.data
  if (data.code !== 0) {
    throw new Error(data.message || "获取播放链接失败")
  }
  const durl = data.data?.durl
  if (!durl || !Array.isArray(durl) || durl.length === 0) {
    throw new Error("无播放链接")
  }
  const playUrl = durl[0]?.url
  if (!playUrl || !playUrl.startsWith("http")) {
    throw new Error("无效播放链接")
  }
  setCachedUrl(bvid, "video", playUrl)
  return playUrl
}

// --- 获取音频播放链接 ---
async function getAudioUrl(sid) {
  const cached = getCachedUrl(sid, "audio")
  if (cached) return cached

  const url = `https://www.bilibili.com/audio/music-service-c/web/url?sid=${encodeURIComponent(sid)}`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("音频链接返回格式异常")
  }
  const data = res.data
  if (data.code !== 0) {
    throw new Error(data.message || "获取音频链接失败")
  }
  const playUrl = data.data?.cdns?.[0] || data.data?.url
  if (!playUrl || !playUrl.startsWith("http")) {
    throw new Error("无效音频链接")
  }
  setCachedUrl(sid, "audio", playUrl)
  return playUrl
}

// --- 哔哩哔哩搜索（视频）---
async function searchBilibiliVideo(keyword, page = 1, limit = 20) {
  const url = `https://api.bilibili.com/x/web-interface/search/type?keyword=${encodeURIComponent(keyword)}&search_type=video&page=${page}`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("搜索返回格式异常")
  }
  const data = res.data
  if (data.code !== 0) {
    throw new Error(data.message || "搜索失败")
  }
  const videos = data.data?.result || []
  const results = []
  for (const item of videos) {
    const bvid = item.bvid || ""
    if (!bvid) continue
    results.push({
      id: bvid,
      name: item.title ? item.title.replace(/<[^>]+>/g, "") : "未知标题",
      singer: item.author || "未知UP主",
      album: "哔哩哔哩视频",
      duration: item.duration ? parseDuration(item.duration) : 0,
      cover: item.pic || "",
      source: "bl",
      type: "video"
    })
  }
  return results
}

// --- 哔哩哔哩搜索（音频）---
async function searchBilibiliAudio(keyword, page = 1, limit = 20) {
  const url = `https://api.bilibili.com/x/web-interface/search/type?keyword=${encodeURIComponent(keyword)}&search_type=audio&page=${page}`
  const res = await httpRequest(url)
  if (res.type !== "json" || !res.data || typeof res.data !== "object") {
    throw new Error("搜索返回格式异常")
  }
  const data = res.data
  if (data.code !== 0) {
    throw new Error(data.message || "搜索失败")
  }
  const audios = data.data?.result || []
  const results = []
  for (const item of audios) {
    const sid = String(item.id || "")
    if (!sid) continue
    results.push({
      id: sid,
      name: item.title ? item.title.replace(/<[^>]+>/g, "") : "未知标题",
      singer: item.uname || "未知UP主",
      album: "哔哩哔哩音频",
      duration: item.duration || 0,
      cover: item.cover || "",
      source: "bl",
      type: "audio"
    })
  }
  return results
}

function parseDuration(durationStr) {
  if (!durationStr) return 0
  const parts = String(durationStr).split(":")
  if (parts.length === 2) {
    return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10)
  } else if (parts.length === 3) {
    return parseInt(parts[0], 10) * 3600 + parseInt(parts[1], 10) * 60 + parseInt(parts[2], 10)
  }
  return 0
}

// --- 主处理逻辑 ---
on(EVENT_NAMES.request, ({ action, source, info }) => {
  if (action === "musicUrl") {
    if (!info?.musicInfo) return Promise.reject(new Error("参数不完整"))
    const musicInfo = info.musicInfo
    const id = musicInfo.id || musicInfo.songmid || musicInfo.hash || ""
    const type = musicInfo.type || "video"
    if (!id) return Promise.reject(new Error("缺少ID"))

    if (DEV_ENABLE) console.log("[哔哩哔哩音源] 获取链接:", id, type, musicInfo.name)

    if (type === "audio") {
      return getAudioUrl(id).then(url => Promise.resolve(url)).catch(err => Promise.reject(err))
    } else {
      return getVideoUrl(id).then(url => Promise.resolve(url)).catch(err => Promise.reject(err))
    }
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
  bl: {
    name: "哔哩哔哩",
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
