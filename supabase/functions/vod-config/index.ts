// supabase/functions/vod-config/index.ts - 简单的配置文件获取服务
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
  // 处理CORS预检请求
  if (req.method === 'OPTIONS') {
    return new Response('ok', {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      }
    })
  }

  const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
  )

  try {
    // 直接从vod-sources存储桶获取onetv-api-movie.json配置文件
    const { data, error } = await supabase.storage
      .from('vod-sources')
      .download('onetv-api-movie.json')

    if (error) {
      throw new Error(`配置文件不存在: ${error.message}`)
    }

    const configText = await data.text()
    const config = JSON.parse(configText)

    // 简单验证配置格式
    if (!config.sites || !Array.isArray(config.sites)) {
      throw new Error('配置文件格式错误: 缺少sites数组')
    }

    // 记录访问日志（异步，不影响响应）
    const logData = {
      timestamp: new Date().toISOString(),
      sites_count: config.sites.length,
      parses_count: config.parses?.length || 0,
      user_agent: req.headers.get('user-agent') || 'unknown'
    }
    supabase.from('vod_access_logs').insert(logData).then()

    return new Response(JSON.stringify(config), {
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Cache-Control': 'public, max-age=600' // 缓存10分钟
      }
    })

  } catch (error) {
    console.error('Config error:', error)

    return new Response(JSON.stringify({
      code: 500,
      msg: `配置获取失败: ${error.message}`,
      timestamp: new Date().toISOString()
    }), {
      status: 500,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      }
    })
  }
})
