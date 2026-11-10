package com.example.liuxueba.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.liuxueba.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Requires the real API and PostgreSQL described in backend/README.md. */
class AuthIntegrationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun registerLoginLoadCurrentUserAndRetryWrongPassword() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val needsLocalPermission = Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_LOCAL_NETWORK
        ) != PackageManager.PERMISSION_GRANTED
        val suffix = System.currentTimeMillis().toString()
        val username = "同学${suffix.takeLast(10)}"
        val email = "verification-android-$suffix@example.com"
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("登录 / 注册").performClick()
        compose.onNodeWithText("还没有账号？立即注册").performScrollTo().performClick()
        field("用户名").performScrollTo().performTextInput(username)
        field("邮箱").performScrollTo().performTextInput(email)
        field("密码").performScrollTo().performTextInput("study2026")
        field("确认密码").performScrollTo().performTextInput("study2026")
        field("确认密码").performImeAction()
        if (needsLocalPermission) {
            clickSystemPermissionButton("permission_deny_button")
            awaitText("需要网络访问权限")
            compose.onNodeWithText("知道了").performClick()
            compose.onNodeWithText("注册").performScrollTo().performClick()
            clickSystemPermissionButton("permission_allow_button")
        }
        awaitText("注册成功")
        compose.onNodeWithText("知道了").performClick()

        field("密码").performScrollTo().performTextInput("study2026")
        compose.onNodeWithText("登录").performScrollTo().performClick()
        awaitText("你好，$username")
        compose.onNodeWithText(email).assertIsDisplayed()
        compose.onNodeWithText("首页").performClick()
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText(email).assertIsDisplayed()

        compose.onNodeWithText("退出登录").performClick()
        compose.onNodeWithText("登录 / 注册").performClick()
        field("邮箱").performTextInput(email)
        field("密码").performTextInput("wrongpassword")
        compose.onNodeWithText("登录").performScrollTo().performClick()
        awaitText("邮箱或密码不正确")
        compose.onNodeWithText("知道了").performClick()
        field("密码").performScrollTo().performTextReplacement("study2026")
        compose.onNodeWithText("登录").performScrollTo().performClick()
        awaitText("你好，$username")
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    private fun clickSystemPermissionButton(id: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (SystemClock.elapsedRealtime() < deadline) {
            val button = automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId(
                "com.android.permissioncontroller:id/$id"
            )?.firstOrNull()
            if (button != null && button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
            SystemClock.sleep(100)
        }
        throw AssertionError("System permission button did not appear: $id")
    }

    private fun awaitText(text: String) {
        val failureNotices = listOf(
            "暂时无法登录", "暂时无法注册", "邮箱或密码不正确", "账号已被使用",
            "请检查输入内容", "网络连接失败", "需要网络访问权限"
        ).filterNot { it == text }
        compose.waitUntil(timeoutMillis = 30_000) {
            (failureNotices + text).any {
                compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty()
            }
        }
        val actualNotice = failureNotices.firstOrNull {
            compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("Expected '$text', received '$actualNotice'", actualNotice == null)
        compose.onNodeWithText(text).assertIsDisplayed()
    }
}
