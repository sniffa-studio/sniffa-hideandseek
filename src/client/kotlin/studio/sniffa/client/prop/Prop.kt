package studio.sniffa.client.prop

import com.geckolib.animatable.GeoAnimatable
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.animation.AnimationController
import com.geckolib.animation.RawAnimation
import com.geckolib.animation.state.AnimationTest
import com.geckolib.util.GeckoLibUtil
import studio.sniffa.prop.PropKind

class Prop(
    val kind: PropKind,
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float,
    seconds: Int,
) : GeoAnimatable {

    private var lifetime: Double = seconds.toDouble()

    var placed = false

    var lastFrameNanos = 0L

    var lastSparkleNanos = 0L

    private val placedAt = System.currentTimeMillis()

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    val age: Double get() = (System.currentTimeMillis() - placedAt) / 1000.0

    fun alive(): Boolean = age < lifetime

    fun endSoon() {
        lifetime = minOf(lifetime, age + PropKind.RETRACT_SECONDS)
    }

    private val retracting: Boolean
        get() = kind.retractAnimation != null && age >= lifetime - PropKind.RETRACT_SECONDS

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        val opening = RawAnimation.begin()
        kind.deployAnimation?.let { opening.thenPlay(it) }
        kind.idleAnimation?.let { opening.thenLoop(it) }

        val closing = kind.retractAnimation?.let { RawAnimation.begin().thenPlayAndHold(it) }

        controllers.add(
            AnimationController<Prop>(CONTROLLER) { test: AnimationTest<Prop> ->
                test.setAndContinue(if (retracting && closing != null) closing else opening)
            }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    private companion object {
        const val CONTROLLER = "prop"

    }
}
