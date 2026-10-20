# language: es
Característica: Prueba de Humo
  Como desarrollador
  Quiero verificar que el sistema responde
  Para asegurar que la aplicación arranca y la seguridad está activa

  @principal
  Escenario: Verificar respuesta del sistema
    Dado que el sistema está operativo
    Cuando intento consultar mi perfil
    Entonces el sistema responde con un código de estado 401
