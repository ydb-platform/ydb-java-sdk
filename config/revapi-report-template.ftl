<#if reports?has_content>
```
<#list analysis.oldApi.archives as archive>${archive.name}<#sep>, </#list>
<#list analysis.newApi.archives as archive>${archive.name}<#sep>, </#list>
```

<#list reports as report>
>***Old: ${report.oldElement!"<none>"}***
>***New: ${report.newElement!"<none>"}***
    <#list report.differences as diff>
> ${diff.code}<#if diff.description??>: ${diff.description}</#if>
    </#list>

</#list>
---
</#if>
