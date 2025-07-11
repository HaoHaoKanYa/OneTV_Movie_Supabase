// supabase/functions/vod-config/index.ts - OneTV点播配置获取服务
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey, x-client-info',
}

serve(async (req) => {
  console.log(`🔗 OneTV VOD Config Request: ${req.method} ${req.url}`)
  console.log(`📋 Request Headers:`, Object.fromEntries(req.headers.entries()))

  // 处理CORS预检请求
  if (req.method === 'OPTIONS') {
    console.log('✅ CORS preflight request handled')
    return new Response('ok', { headers: corsHeaders })
  }

  // 检查环境变量
  const supabaseUrl = Deno.env.get('SUPABASE_URL')
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

  console.log(`🔧 Environment check:`)
  console.log(`   SUPABASE_URL: ${supabaseUrl ? 'SET' : 'MISSING'}`)
  console.log(`   SERVICE_ROLE_KEY: ${serviceRoleKey ? 'SET' : 'MISSING'}`)

  if (!supabaseUrl || !serviceRoleKey) {
    console.error('❌ Missing environment variables')
    return new Response(JSON.stringify({
      code: 500,
      msg: '服务器配置错误：缺少环境变量',
      timestamp: new Date().toISOString()
    }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }

  const supabase = createClient(supabaseUrl, serviceRoleKey)

  try {
    console.log(`📦 生成私密存储桶配置文件访问链接...`)
    // 为私密存储桶生成带签名的访问链接（有效期1小时）
    console.log(`🔗 生成onetv-api-movie.json带签名访问链接...`)
    const { data: urlData, error: urlError } = await supabase.storage
      .from('vod-sources')
      .createSignedUrl('onetv-api-movie.json', 3600) // 1小时有效期

    if (urlError || !urlData?.signedUrl) {
      console.error('❌ 无法生成配置文件签名链接:', urlError)
      throw new Error(`无法生成配置文件签名链接: ${urlError?.message}`)
    }

    const configUrl = urlData.signedUrl
    console.log(`✅ 配置文件签名链接生成成功: ${configUrl}`)

    // 验证文件是否存在（可选检查）
    try {
      const { data: files, error: listError } = await supabase.storage
        .from('vod-sources')
        .list()

      if (!listError && files) {
        const fileExists = files.some(f => f.name === 'onetv-api-movie.json')
        if (!fileExists) {
          console.warn('⚠️ 配置文件可能不存在于存储桶中')
          console.log('📋 vod-sources存储桶中的文件:', files.map(f => f.name))
        } else {
          console.log('✅ 配置文件存在于存储桶中')
        }
      }
    } catch (checkError) {
      console.warn('⚠️ 文件存在性检查失败，但继续返回链接:', checkError)
    }

    // 简化响应数据，直接返回TVBOX兼容的配置URL
    const responseData = {
      config_url: configUrl,
      message: 'OneTV TVBOX配置链接',
      timestamp: new Date().toISOString(),
      type: 'tvbox_config'
    }

    // 记录访问日志（异步，不影响响应）
    const logData = {
      timestamp: new Date().toISOString(),
      user_agent: req.headers.get('user-agent') || 'unknown',
      success: true,
      config_url: configUrl,
      access_type: 'tvbox_config_link'
    }
    supabase.from('vod_access_logs').insert(logData).then()

    console.log(`🎉 TVBOX配置链接返回成功`)
    return new Response(JSON.stringify(responseData), {
      headers: {
        ...corsHeaders,
        'Content-Type': 'application/json',
        'Cache-Control': 'public, max-age=3600' // 缓存1小时，配置链接相对稳定
      }
    })

  } catch (error) {
    console.error('💥 配置获取失败:', error)

    // 记录失败日志
    const errorLogData = {
      timestamp: new Date().toISOString(),
      user_agent: req.headers.get('user-agent') || 'unknown',
      success: false,
      error_message: error.message,
      config_url: null,
      access_type: 'tvbox_config_link_failed'
    }
    supabase.from('vod_access_logs').insert(errorLogData).then()

    return new Response(JSON.stringify({
      code: 500,
      msg: `配置获取失败: ${error.message}`,
      timestamp: new Date().toISOString(),
      debug: {
        error_type: error.constructor.name,
        error_message: error.message
      }
    }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})
