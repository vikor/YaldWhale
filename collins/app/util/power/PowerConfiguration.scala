package util
package power

case class InvalidPowerConfigurationException(message: String, key: Option[String] = None) extends RuntimeException(message)

case class PowerConfiguration(
  unitsRequired: Int,
  useAlphabeticNames: Boolean,
  private val _components: Set[String],
  private val _uniques: Set[String]
) extends MessageHelper("powerconfiguration") {
  import PowerConfiguration.Messages._

  if (unitsRequired < 0 || unitsRequired > 18)
    throw InvalidPowerConfigurationException(UnitsRequired)

  protected def validateComponents(set: Set[String]) {
    val (empty, nonEmpty) = set.partition(_.trim.isEmpty)
    val (trimmed, ok) = set.partition(s => s.trim != s)
    if (empty.size != 0)
      throw InvalidPowerConfigurationException(ComponentsUnspecified)
    else if (nonEmpty.size == 0)
      throw InvalidPowerConfigurationException(ComponentsUnspecified)
    else if (trimmed.size != 0)
      throw InvalidPowerConfigurationException(InvalidComponent(trimmed.head))
  }

  if (unitsRequired > 0) {
    validateComponents(_components)
    if (_uniques.size > 0) {
      validateComponents(_uniques)
      val missingComponents = _uniques &~ _components
      if (missingComponents.size != 0)
        throw InvalidPowerConfigurationException(InvalidUnique(missingComponents.head))
    }
  }

  val components: Set[Symbol] = _components.map(s => Symbol(s.toUpperCase))
  val uniqueComponents: Set[Symbol] = _uniques.map(s => Symbol(s.toUpperCase))
  lazy val units: PowerUnits =
    PowerUnits((0 until unitsRequired).map(id => PowerUnit(this, id)))

  def hasComponent(component: Symbol) = components.contains(Symbol(component.name.toUpperCase))
}

object PowerConfiguration extends FeatureConfig("powerconfiguration") {

  val DefaultUnitsRequired = feature("unitsRequired").ifSet { f =>
    f.toInt(-1)
  }.filter(i => i >= 0 && i <= 18).getOrElse(1)
  val DefaultUseAlphabeticNames = feature("useAlphabeticNames").ifSet(_.enabled).getOrElse(true)
  val DefaultComponents = feature("unitComponents").toSet
  val DefaultUniques = feature("uniqueComponents").toSet

  object Messages extends MessageHelper("powerconfiguration") {
    def InvalidComponent(ct: String) =
      messageWithDefault("unitComponents.invalid",
                         "Specified unitComponent %s is invalid".format(ct), ct)
    def ComponentLabel(ct: String, idx: String) =
      messageWithDefault(labelFor(ct), defaultLabel(ct, idx), idx)
    val ComponentsUnspecified =
      messageWithDefault("unitComponents.unspecified", "unitComponents not specified but required")
    def InvalidUnique(ut: String) =
      messageWithDefault("uniqueComponents.invalid",
                         "Specified uniqueComponent %s is invalid".format(ut), ut)
    val UnitsRequired =
      messageWithDefault("unitsRequired.range", "unitsRequired must be >= 0 && <= 18")
    def MissingData(key: String, label: String) =
      messageWithDefault("missingData", defaultMissing(key, label), key, label)
    def ValidationMissingRequired(ct: String, key: String) =
      messageWithDefault("validation.missingRequired", "Missing power component", ct, key)
    def ValidationNonUnique(ct: String, seenValue: String) =
      messageWithDefault("validation.nonUnique", "Duplicate value found", ct, seenValue)

    private def labelFor(ct: String) = "unit.%s.label".format(ct.toLowerCase)
    private def defaultLabel(ct: String, idx: String) = "%s %s".format(ct, idx)
    private def defaultMissing(k: String, l: String) =
      "Did not find value for %s, required for %s".format(k, l)
  }

  def apply(): PowerConfiguration = {
    new PowerConfiguration(
      DefaultUnitsRequired, DefaultUseAlphabeticNames, DefaultComponents, DefaultUniques
    )
  }

  def validate() {
    // validates config on initialization
    val config = apply()
    PowerUnits().foreach { unit => unit.foreach { component =>
      component.meta
    }}
  }

  def apply(cfg: Option[PowerConfiguration]): PowerConfiguration = {
    cfg.getOrElse(apply())
  }
}
