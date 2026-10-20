# language: es
Característica: Registro de Entrenamientos
  Como jugador
  Quiero registrar mis sesiones de entrenamiento
  Para ganar XP y avanzar en Kinet

  Antecedentes:
    Dado que el sistema está operativo
    Y que existe un jugador registrado con gamertag "leo_run" y correo "leo@test.com"

  @principal
  Escenario: Un entrenamiento válido otorga XP
    Cuando registro un entrenamiento de "FUERZA" de 30 minutos con intensidad "MEDIA"
    Entonces el sistema responde con un código de estado 200
    Y el entrenamiento queda en estado "VALIDADO"
    Y el entrenamiento otorga 90 XP
    Y el entrenamiento queda guardado en la base de datos con 30 minutos
