package studio.sniffa.client.prop

import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.manager.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.animation.state.AnimationTest
import software.bernie.geckolib.util.GeckoLibUtil
import studio.sniffa.prop.PropKind

class Prop(
    val kind: PropKind,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    private val seconds: Int,
) : GeoAnimatable {

    private val placedAt = System.currentTimeMillis()

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    val age: Double get() = (System.currentTimeMillis() - placedAt) / 1000.0

    fun alive(): Boolean = age < seconds

    private val retracting: Boolean
        get() = kind.retractAnimation != null && age >= seconds - PropKind.RETRACT_SECONDS

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
