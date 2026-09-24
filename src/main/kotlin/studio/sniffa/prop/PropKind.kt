package studio.sniffa.prop

enum class PropKind(
    val id: String,
    val scale: Float,
    private val idle: String?,
    private val deploy: String? = null,
    private val retract: String? = null,
) {

    RADAR("radar", 1.0f, "sweep", "deploy", "retract"),
    ;

    val asset: String get() = id

    val texture: String get() = "textures/prop/$id.png"

    val idleAnimation: String? get() = blockbenchName(idle)

    val deployAnimation: String? get() = blockbenchName(deploy)

    val retractAnimation: String? get() = blockbenchName(retract)

    private fun blockbenchName(name: String?): String? = name?.let { "animation.$id.$it" }

    companion object {
        const val RETRACT_SECONDS = 0.5

        private val BY_ID = entries.associateBy { it.id }

        fun byId(id: String): PropKind? = BY_ID[id]
    }
}
