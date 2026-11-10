package com.example.liuxueba.auth

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso
import com.example.liuxueba.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun openLogin() {
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("登录 / 注册").performClick()
    }

    @Test
    fun registrationAndBackNavigationReturnToProfileAndMainTabs() {
        compose.onNodeWithText("还没有账号？立即注册").performScrollTo().performClick()
        field("用户名").assertIsDisplayed()
        field("确认密码").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("← 返回").performScrollTo().performClick()
        compose.onNodeWithText("欢迎来到留学吧").assertIsDisplayed()
        compose.onNodeWithText("← 返回").performScrollTo().performClick()
        compose.onNodeWithText("个人中心").assertIsDisplayed()

        compose.onNodeWithText("首页").performClick()
        compose.onNodeWithText("搜索留学信息").assertExists()
        compose.onNodeWithText("社区").performClick()
        compose.onNodeWithText("留学社区").assertIsDisplayed()
        compose.onNodeWithText("院校").performClick()
        compose.onNodeWithText("院校与专业").assertIsDisplayed()
    }

    @Test
    fun loginRejectsInvalidInputsAndShowsUserFacingMessageForValidForm() {
        compose.onNodeWithText("登录").assertIsNotEnabled()
        field("邮箱").performTextInput("invalid-email")
        compose.onNodeWithText("请输入有效的邮箱地址").assertExists()
        field("密码").performTextInput("short")
        compose.onNodeWithText("密码需要 8—64 个字符").assertExists()
        compose.onNodeWithText("登录").performScrollTo().assertIsNotEnabled()

        field("邮箱").performScrollTo().performTextReplacement("student@example.com")
        field("密码").performScrollTo().performTextReplacement("study2026")
        compose.onNodeWithText("登录").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("暂时无法登录").assertIsDisplayed()
        compose.onNodeWithText("请稍后重试。").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithText("登录").performScrollTo().assertIsEnabled()
    }

    @Test
    fun registrationRevalidatesConfirmationWhenPasswordChanges() {
        compose.onNodeWithText("还没有账号？立即注册").performScrollTo().performClick()
        field("用户名").performScrollTo().performTextInput("留学同学")
        field("邮箱").performScrollTo().performTextInput("student@example.com")
        field("密码").performScrollTo().performTextInput("study2026")
        field("确认密码").performScrollTo().performTextInput("study2026")
        compose.onNodeWithText("注册").performScrollTo().assertIsEnabled()

        field("密码").performScrollTo().performTextReplacement("newstudy2026")
        compose.onNodeWithText("两次输入的密码不一致").assertExists()
        compose.onNodeWithText("注册").performScrollTo().assertIsNotEnabled()
        field("确认密码").performScrollTo().performTextReplacement("newstudy2026")
        field("确认密码").performImeAction()
        compose.onNodeWithText("暂时无法注册").assertIsDisplayed()
        compose.onNodeWithText("请稍后重试。").assertIsDisplayed()
    }

    @Test
    fun registrationPasswordsAreMaskedAndCanBeRevealedIndependently() {
        compose.onNodeWithText("还没有账号？立即注册").performScrollTo().performClick()
        field("密码").performScrollTo().performTextInput("study2026")
        field("确认密码").performScrollTo().performTextInput("study2026")
        field("密码").assertTextValue("•••••••••")
        field("确认密码").assertTextValue("•••••••••")

        compose.onNodeWithContentDescription("显示密码").performScrollTo().performClick()
        field("密码").assertTextValue("study2026")
        field("确认密码").assertTextValue("•••••••••")

        compose.onNodeWithContentDescription("隐藏密码").performScrollTo().performClick()
        field("密码").assertTextValue("•••••••••")
        compose.onNodeWithContentDescription("显示确认密码").performScrollTo().performClick()
        field("确认密码").assertTextValue("study2026")
    }

    @Test
    fun keyboardNextMovesThroughRegistrationFields() {
        compose.onNodeWithText("还没有账号？立即注册").performScrollTo().performClick()
        field("用户名").performScrollTo().performClick().performImeAction()
        field("邮箱").assertIsFocused().performImeAction()
        field("密码").assertIsFocused().performImeAction()
        field("确认密码").assertIsFocused()
    }

    @Test
    fun systemBackReturnsFromRegistrationToLoginThenProfile() {
        compose.onNodeWithText("还没有账号？立即注册").performScrollTo().performClick()
        Espresso.pressBack()
        compose.onNodeWithText("欢迎来到留学吧").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("个人中心").assertIsDisplayed()
    }

    @Test
    fun activityRecreationKeepsInMemoryFormDraft() {
        field("邮箱").performTextInput("student@example.com")
        field("密码").performTextInput("study2026")
        compose.activityRule.scenario.recreate()

        field("邮箱").assertTextValue("student@example.com")
        compose.onNodeWithText("登录").performScrollTo().assertIsEnabled()
        field("密码").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test
    fun leavingAuthenticationClearsPasswordDraft() {
        field("邮箱").performTextInput("student@example.com")
        field("密码").performTextInput("study2026")
        compose.onNodeWithText("← 返回").performScrollTo().performClick()
        compose.onNodeWithText("登录 / 注册").performClick()

        field("密码").assertTextValue("")
        compose.onNodeWithText("登录").assertIsNotEnabled()
    }

    private fun field(label: String) =
        compose.onNode(hasSetTextAction() and hasText(label))

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertTextValue(value: String) =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)))
}
