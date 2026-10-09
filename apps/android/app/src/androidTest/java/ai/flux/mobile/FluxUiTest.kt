package ai.flux.mobile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.Path
import android.graphics.Color
import android.os.Environment
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import android.graphics.Point
import androidx.test.uiautomator.UiDevice
import androidx.test.core.app.ActivityScenario
import ai.flux.mobile.security.FluxVaultActivity
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FluxUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val permissions=GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.POST_NOTIFICATIONS)
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun evidence(name:String){val context=compose.activity;val dir=File(context.getExternalFilesDir(null),"evidence");dir.mkdirs();device.takeScreenshot(File(dir,"$name.png"))}
    @Before fun dismissOnboarding(){
        compose.waitForIdle()
        if(compose.onAllNodesWithText("AGORA NÃO").fetchSemanticsNodes().isNotEmpty())compose.onNodeWithText("AGORA NÃO").performClick()
    }
    @Test fun startupNavigationAndPersistentAccent(){
        compose.onNodeWithTag("flux-home").assertExists();evidence("home")
        compose.onNodeWithContentDescription("Abrir ajustes").performClick()
        compose.onNodeWithText("SISTEMA").assertExists()
        compose.onNodeWithText("APARÊNCIA").performScrollTo()
        compose.onNodeWithTag("accent-cyan").performClick()
        Assert.assertEquals("cyan",(compose.activity.application as FluxApplication).workspace.accentKey())
        evidence("settings")
        compose.onNode(hasText("Workspace") and hasClickAction()).performClick()
        compose.onNodeWithText("Memória").performClick()
        compose.onNodeWithText("Memórias autorizadas").assertExists()
        compose.onNodeWithText("Pareie o aparelho em Ajustes para acessar este módulo.").assertExists()
        evidence("memory-unpaired")
        compose.onNodeWithText("Conexões").performClick()
        compose.onNodeWithText("FLUX Connect").assertExists();evidence("connect-unpaired")
        compose.onNodeWithText("Segurança").performScrollTo().performClick()
        compose.onNodeWithText("Abrir cofre seguro").assertExists();evidence("privacy")
        compose.onNodeWithText("Chat").performClick()
        compose.onNodeWithTag("chat-input").assertExists()
        device.pressHome()
        device.swipe(device.displayWidth/2,device.displayHeight*4/5,device.displayWidth/2,device.displayHeight/5,30)
        val icon=device.wait(Until.findObject(By.text("FLUX")),5000)
        Assert.assertNotNull("Installed FLUX icon must appear in launcher app drawer",icon)
        evidence("launcher-installed-drawer")
        icon!!.drag(Point(device.displayWidth/2,device.displayHeight/2),500)
        device.pressHome()
        device.waitForIdle()
        evidence("launcher-home")
    }
    @Test fun adaptiveIconMasks(){
        val context=compose.activity
        val icon=context.packageManager.getApplicationIcon(context.packageName)
        Assert.assertTrue(icon is AdaptiveIconDrawable)
        val dir=File(context.getExternalFilesDir(null),"evidence");dir.mkdirs()
        val masks=listOf("circle","round-square","squircle")
        for(name in masks){val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);val c=Canvas(bitmap)
            val path=Path();if(name=="circle")path.addCircle(128f,128f,128f,Path.Direction.CW)
            else path.addRoundRect(0f,0f,256f,256f,if(name=="squircle")70f else 40f,if(name=="squircle")70f else 40f,Path.Direction.CW)
            c.clipPath(path); val adaptive=icon as AdaptiveIconDrawable
            adaptive.background.setBounds(-64,-64,320,320);adaptive.background.draw(c)
            adaptive.foreground.setBounds(-64,-64,320,320);adaptive.foreground.draw(c)
            Assert.assertNotEquals(Color.WHITE,bitmap.getPixel(128,250))
            File(dir,"icon-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
    }
    @Test fun vaultIsProtectedAndDoesNotStorePlaintext(){
        ActivityScenario.launch(FluxVaultActivity::class.java).use{scenario->scenario.onActivity{activity->
            Assert.assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            Assert.assertFalse(activity.getSharedPreferences("flux_vault_encrypted",0).all.values.any{it.toString().contains("password")})
        }}
    }
}
