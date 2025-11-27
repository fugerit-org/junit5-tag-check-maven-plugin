package org.fugerit.java.junit5.tag.check.facade;

import org.fugerit.java.core.function.SafeFunction;
import org.fugerit.java.doc.base.process.DocProcessContext;
import org.fugerit.java.doc.freemarker.process.FreemarkerDocProcessConfig;
import org.fugerit.java.doc.freemarker.process.FreemarkerDocProcessConfigFacade;
import org.fugerit.java.junit5.tag.check.model.ReportHelper;

import java.io.OutputStream;

/**
 * DocHelper, version : auto generated on 2025-11-26 21:15:51.279
 */
public class DocHelper {

     /*
      * FreemarkerDocProcessConfig is thread-safe and should be initialized once for each config file.
      * 
      * Consider using a @ApplicationScoped or Singleton approach.
      */
     private static final FreemarkerDocProcessConfig docProcessConfig = FreemarkerDocProcessConfigFacade
            .loadConfigSafe("cl://junit5-tag-check-maven-plugin/fm-doc-process-config.xml");

     public static void generateReport(String handlerId, ReportHelper reportHelper, OutputStream os ) {
          SafeFunction.apply( () -> docProcessConfig.
                  fullProcess( "report",
                          DocProcessContext.newContext( "report", reportHelper )
                                  .withDocType( handlerId ), handlerId, os ) );
     }


}
