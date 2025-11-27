<?xml version="1.0" encoding="utf-8"?>
<doc
        xmlns="http://javacoredoc.fugerit.org"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://javacoredoc.fugerit.org https://www.fugerit.org/data/java/doc/xsd/doc-2-1.xsd" >

    <#assign docTitle="Executed Test Tag Report">
    <#assign passForeColor="#00aa00">
    <#assign failForeColor="#aa0000">
    <#assign errorForeColor="#aa6060">
    <#assign skipForeColor="#909090">
    <#assign defaultTableBorderSize="0">
    
    <#assign testTagMap=report.testTagMap/>
    <#assign tagsSummary=report.tagsSummary/>

    <metadata>
        <!-- Margin for document : left;right;top;bottom -->
        <info name="margins">10;10;10;10</info>
        <!-- documenta meta information -->
        <info name="doc-title">${docTitle}</info>
        <info name="doc-subject">Report</info>
        <info name="doc-author">fugerit79</info>
        <info name="doc-language">en</info>
        <info name="page-width">29.7cm</info>
        <info name="page-height">21cm</info>
        <info name="html-css-style">
            body { font-family: Arial, sans-serif; margin: 20px; }
            table { border-collapse: collapse; width: 100%; margin: 20px 0; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #4CAF50; color: white; }
            tr:nth-child(even) { background-color: #f2f2f2; }
            .pass { color: green; }
            .fail { color: red; }
            .error { color: orange; }
            .skip { color: gray; }
            .tag { background-color: #e7f3ff; padding: 2px 8px; border-radius: 3px; margin: 2px; display: inline-block; }
        </info>
    </metadata>
    <body>
    <h head-level="1" style="bold">${docTitle}</h>

    <h head-level="2" style="bold" space-before="20">Summary</h>

    <table columns="2" colwidths="50;50"  width="100" id="summary-table">
        <row header="true">
            <cell border-width="${defaultTableBorderSize}"><phrase>Metric</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>Count</phrase></cell>
        </row>
        <row>
            <cell border-width="${defaultTableBorderSize}"><phrase>Total Tests</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>${testTagMap?size}</phrase></cell>
        </row>
        <row>
            <cell border-width="${defaultTableBorderSize}"><phrase>Passed</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase <#if docType == 'html'>class="pass"<#else>fore-color="${passForeColor}"</#if>>${report.summaryPass}</phrase></cell>
        </row>
        <row>
            <cell border-width="${defaultTableBorderSize}"><phrase>Failed</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase <#if docType == 'html'>class="fail"<#else>fore-color="${failForeColor}"</#if>>${report.summaryFail}</phrase></cell>
        </row>
        <row>
            <cell border-width="${defaultTableBorderSize}"><phrase>Errors</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase <#if docType == 'html'>class="error"<#else>fore-color="${errorForeColor}"</#if>>${report.summaryError}</phrase></cell>
        </row>
    </table>

    <h head-level="2" style="bold" space-before="20">Tags Summary</h>

    <table columns="2" colwidths="50;50"  width="100" id="tags-summary-table">
        <row header="true">
            <cell border-width="${defaultTableBorderSize}"><phrase>Tag</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>Tests</phrase></cell>
        </row>
        <#list tagsSummary?keys as currentTag >
        <row>
            <cell border-width="${defaultTableBorderSize}"><phrase>${currentTag}</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>${tagsSummary[currentTag]?size}</phrase></cell>
        </row>
        </#list>
    </table>

    <h head-level="2" style="bold" space-before="20">All Executed Tests</h>

    <table columns="4" colwidths="15;55;15;15"  width="100" id="all-tests-table">
        <row header="true">
            <cell border-width="${defaultTableBorderSize}"><phrase>Status</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>Test</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>Tags</phrase></cell>
            <cell border-width="${defaultTableBorderSize}"><phrase>Time</phrase></cell>
        </row>
        <#list testTagMap?keys as currentTest>
            <#assign currentClassName><#if currentTest.failed>class="fail"<#elseif currentTest.error>class="error"<#else>class="pass"</#if></#assign>
            <#assign currentStatusIcon><#if currentTest.failed>Fail<#elseif currentTest.error>Error<#elseif currentTest.skipped>Skipped<#else>Passed</#if></#assign>
            <row>
                <cell border-width="${defaultTableBorderSize}" ${currentClassName}><phrase>${currentStatusIcon}</phrase></cell>
                <cell border-width="${defaultTableBorderSize}"><phrase>${currentTest.className}&#8203;#${currentTest.methodName}</phrase></cell>
                <cell border-width="${defaultTableBorderSize}"><#list currentTest.tags as currentTag><phrase class="tag">${currentTag}</phrase></#list></cell>
                <cell border-width="${defaultTableBorderSize}"><phrase>${currentTest.time}s</phrase></cell>
            </row>
        </#list>
    </table>

    </body>

</doc>