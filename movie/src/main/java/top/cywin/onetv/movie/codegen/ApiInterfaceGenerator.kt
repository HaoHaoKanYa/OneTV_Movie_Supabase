package top.cywin.onetv.movie.codegen

import com.squareup.kotlinpoet.*
import top.cywin.onetv.movie.data.models.VodSite

/**
 * API接口生成器
 * 为每个站点生成类型安全的API接口
 */
object ApiInterfaceGenerator {
    private const val GENERATED_PACKAGE = "top.cywin.onetv.movie.generated.api"
    
    fun generateApiInterface(site: VodSite): FileSpec {
        val className = "${site.key.capitalize()}ApiService"
        
        val apiInterface = TypeSpec.interfaceBuilder(className)
            .addFunction(generateSearchFunction(site))
            .addFunction(generateDetailFunction(site))
            .addFunction(generateCategoryFunction(site))
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, className)
            .addType(apiInterface)
            .addFileComment("🤖 自动生成的API接口 - ${site.name}")
            .build()
    }
    
    fun generateApiManager(sites: List<VodSite>): FileSpec {
        val managerClass = TypeSpec.classBuilder("GeneratedApiManager")
            .build()
        
        return FileSpec.builder(GENERATED_PACKAGE, "GeneratedApiManager")
            .addType(managerClass)
            .build()
    }
    
    private fun generateSearchFunction(site: VodSite): FunSpec = FunSpec.builder("search").build()
    private fun generateDetailFunction(site: VodSite): FunSpec = FunSpec.builder("getDetail").build()
    private fun generateCategoryFunction(site: VodSite): FunSpec = FunSpec.builder("getCategory").build()
}
