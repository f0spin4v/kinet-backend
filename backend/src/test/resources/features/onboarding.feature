# language: es
Característica: Onboarding de un nuevo jugador
  Como jugador nuevo
  Quiero crear mi cuenta
  Para empezar a entrenar en Kinet

  Antecedentes:
    Dado que el sistema está operativo

  @principal
  Escenario: Registro exitoso de un nuevo jugador
    Cuando un jugador se registra con gamertag "carlos_r" y correo "carlos.ruiz@test.com"
    Entonces el sistema responde con un código de estado 201
    Y la respuesta debe contener un token de acceso
    Y el jugador aún no tiene nivel de habilidad
    Y el jugador queda guardado en la base de datos

  @principal
  Escenario: Evaluación de nivel inicial
    Dado que existe un jugador registrado con gamertag "ana_fit" y correo "ana@test.com"
    Cuando el jugador responde la evaluación eligiendo el nivel "AVANZADO"
    Entonces el sistema responde con un código de estado 201
    Y su nivel de habilidad es "AVANZADO"
