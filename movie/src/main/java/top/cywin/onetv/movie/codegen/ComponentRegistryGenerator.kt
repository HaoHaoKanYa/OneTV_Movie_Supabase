package top.cywin.onetv.movie.codegen

import com.squareup.kotlinpoet.*
import top.cywin.onetv.movie.data.models.VodConfigResponse

/**
 * 组件注册中心生成器
 * 生成统一的组件管理和注册中心
 */
object ComponentRegistryGenerator {
    private const val GENERATED_PACKAGE = "top.cywin.onetv.movie.generated.registry"
    
    fun generateRegistry(config: VodConfigResponse): FileSpec {
        val registryClass = TypeSpec.classBuilder("GeneratedComponentRegistry")
            .addFunction(generateGetParserManagerFunction())
            .addFunction(generateGetSiteAdapterManagerFunction())
            .addFunction(generateGetCacheManagerFunction())
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, "GeneratedComponentRegistry")
            .addType(registryClass)
            .addFileComment("🤖 自动生成的组件注册中心")
            .build()
    }
    
    private fun generateGetParserManagerFunction(): FunSpec = FunSpec.builder("getParserManager").build()
    private fun generateGetSiteAdapterManagerFunction(): FunSpec = FunSpec.builder("getSiteAdapterManager").build()
    private fun generateGetCacheManagerFunction(): FunSpec = FunSpec.builder("getCacheManager").build()
}
