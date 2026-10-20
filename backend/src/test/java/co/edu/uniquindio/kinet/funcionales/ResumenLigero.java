package co.edu.uniquindio.kinet.funcionales;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestStepFinished;

/**
 * Salida de Cucumber en consola, organizada como Preparación (Dado) → Acción (Cuando) → Verificación (Entonces).
 * Debajo de cada paso muestra lo que anotó la Bitacora: endpoint, datos enviados, código y respuesta, y las
 * consultas a la base de datos. Marca: ✓ pasó, ✗ falló, – no se ejecutó.
 */
public class ResumenLigero implements ConcurrentEventListener {

    private static final String LINEA = "  " + "─".repeat(78);

    private int pasaron;
    private int fallaron;
    private String seccion;

    @Override
    public void setEventPublisher(EventPublisher publicador) {
        publicador.registerHandlerFor(TestCaseStarted.class, this::alEmpezarEscenario);
        publicador.registerHandlerFor(TestStepFinished.class, this::alTerminarPaso);
        publicador.registerHandlerFor(TestCaseFinished.class, this::alTerminarEscenario);
        publicador.registerHandlerFor(TestRunFinished.class, evento -> alTerminar());
    }

    private synchronized void alEmpezarEscenario(TestCaseStarted evento) {
        Bitacora.vaciar();
        seccion = null;
        String uri = evento.getTestCase().getUri().toString();
        String proceso = uri.substring(uri.lastIndexOf('/') + 1).replace(".feature", "");
        System.out.println();
        System.out.println(LINEA);
        System.out.println("  ESCENARIO: " + evento.getTestCase().getName() + "   [" + proceso + "]");
        System.out.println(LINEA);
    }

    private synchronized void alTerminarPaso(TestStepFinished evento) {
        // solo los pasos de Gherkin; los hooks (@Before, @After) no se muestran
        if (!(evento.getTestStep() instanceof PickleStepTestStep paso)) {
            Bitacora.vaciar();
            return;
        }
        String palabra = paso.getStep().getKeyword().trim();
        String nueva = switch (palabra) {
            case "Dado", "Dada", "Dados", "Dadas" -> "1. PREPARACIÓN (Dado)";
            case "Cuando" -> "2. ACCIÓN (Cuando)";
            case "Entonces" -> "3. VERIFICACIÓN (Entonces)";
            default -> seccion; // "Y" y "Pero" siguen en la sección anterior
        };
        if (nueva != null && !nueva.equals(seccion)) {
            seccion = nueva;
            System.out.println("  " + seccion);
        }

        Result resultado = evento.getResult();
        String marca = switch (resultado.getStatus()) {
            case PASSED -> "✓";
            case SKIPPED -> "–";
            default -> "✗";
        };
        System.out.println("    " + marca + " " + palabra + " " + paso.getStep().getText());
        for (String linea : Bitacora.vaciar()) {
            System.out.println("        " + linea);
        }
        if (resultado.getStatus() == Status.FAILED && resultado.getError() != null
                && resultado.getError().getMessage() != null) {
            System.out.println("        ✗ " + resultado.getError().getMessage().lines().findFirst().orElse(""));
        }
    }

    private synchronized void alTerminarEscenario(TestCaseFinished evento) {
        boolean paso = evento.getResult().getStatus() == Status.PASSED;
        if (paso) {
            pasaron++;
        } else {
            fallaron++;
        }
        System.out.println("  Resultado: " + (paso ? "✓ PASÓ" : "✗ FALLÓ"));
    }

    private synchronized void alTerminar() {
        System.out.println();
        System.out.println(LINEA);
        System.out.println("  " + (pasaron + fallaron) + " escenarios: " + pasaron + " pasaron, " + fallaron + " fallaron");
        System.out.println(LINEA);
    }
}
