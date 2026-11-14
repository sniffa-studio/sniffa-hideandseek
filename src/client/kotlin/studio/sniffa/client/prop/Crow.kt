package studio.sniffa.client.prop

import com.geckolib.animatable.GeoAnimatable
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.animation.AnimationController
import com.geckolib.animation.RawAnimation
import com.geckolib.animation.state.AnimationTest
import com.geckolib.util.GeckoLibUtil

class Crow(val offset: Double) : GeoAnimatable {

    var x = 0.0
    var y = 0.0
    var z = 0.0
    var yaw = 0f
    var pitch = 0f
    var flying = false
    var visible = true
    var seconds = 0.0

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController<Crow>(CONTROLLER, TRANSITION_TICKS) { test: AnimationTest<Crow> ->
                test.setAndContinue(if (flying) FLY else IDLE)
            }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    private companion object {
        const val CONTROLLER = "crow"
        const val TRANSITION_TICKS = 3
        val IDLE: RawAnimation = RawAnimation.begin().thenLoop("animation.crow.idle")
        val FLY: RawAnimation = RawAnimation.begin().thenLoop("animation.crow.fly")
    }
}
