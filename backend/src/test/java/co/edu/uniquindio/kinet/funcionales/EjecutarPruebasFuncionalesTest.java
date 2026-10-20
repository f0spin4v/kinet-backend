package co.edu.uniquindio.kinet.funcionales;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME;

/**
 * Punto de entrada de las pruebas funcionales BDD: ejecuta los escenarios de src/test/resources/features
 * con los pasos de este paquete. En consola, una línea por escenario (ResumenLigero); el detalle paso a paso
 * queda en el reporte HTML target/cucumber/reporte.html.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "co.edu.uniquindio.kinet.funcionales")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "co.edu.uniquindio.kinet.funcionales.ResumenLigero, html:target/cucumber/reporte.html")
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
public class EjecutarPruebasFuncionalesTest {
}
