package com.example.liuxueba.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liuxueba.PageContainer
import com.example.liuxueba.R
import com.example.liuxueba.domain.auth.AuthUser

@Composable
fun ProfilePage(user: AuthUser?, onLoginClick: () -> Unit, onLogoutClick: () -> Unit) {
    PageContainer {
        Text("个人中心", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                if (user == null) {
                    Text("欢迎来到留学吧", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("登录后即可发布帖子、评论和收藏内容。")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                        Text("登录 / 注册")
                    }
                } else {
                    Text(stringResource(R.string.profile_greeting, user.nickname), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(user.email)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_logout))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("我的功能", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        listOf("我的帖子", "我的评论", "我的收藏", "账户设置", "关于留学吧").forEach { ProfileItem(it) }
    }
}

@Composable
private fun ProfileItem(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Text("›", color = MaterialTheme.colorScheme.secondary)
        }
    }
}
