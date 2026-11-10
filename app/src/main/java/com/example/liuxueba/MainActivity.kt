
package com.example.liuxueba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liuxueba.ui.theme.LiuXueBaTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import com.example.liuxueba.ui.auth.AuthRoute
import com.example.liuxueba.ui.auth.AuthViewModel
import com.example.liuxueba.di.AppContainer
import com.example.liuxueba.ui.profile.ProfilePage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authViewModel = ViewModelProvider(
            this, AuthViewModel.factory(AppContainer.authRepository)
        )[AuthViewModel::class.java]
        setContent {
            LiuXueBaTheme {
                MainScreen(authViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAuthPage by rememberSaveable { mutableStateOf(false) }

    if (showAuthPage) {
        AuthRoute(viewModel = authViewModel, onExit = { showAuthPage = false })
        return
    }

    val tabs = listOf("首页", "社区", "院校", "我的")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {},
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomePage()
                1 -> CommunityPage()
                2 -> InstitutionPage()
                3 -> ProfilePage(
                    user = authViewModel.currentUser,
                    onLogoutClick = authViewModel::logout,
                    onLoginClick = {
                        authViewModel.reset()
                        showAuthPage = true
                    }
                )
            }
        }
    }
}

// 通用页面滚动布局
@Composable
fun PageContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        content = content
    )
}

// 首页
@Composable
fun HomePage() {
    var keyword by remember { mutableStateOf("") }

    PageContainer {
        Text(
            "留学吧",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text("让留学信息交流更简单")

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "探索你的留学之路",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text("从英国院校到专业选择，找到适合自己的留学方向。")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("搜索留学信息", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索大学、专业或留学经验") },
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        Text("快捷入口", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard("英国院校", "查找大学", Modifier.weight(1f))
            QuickCard("热门专业", "了解课程", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        Text("热门留学讨论", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        PostCard(
            "英国硕士申请应该如何准备？",
            "分享申请时间规划、材料准备和选校经验。",
            "申请经验"
        )

        Spacer(Modifier.height(12.dp))

        PostCard(
            "英国留学一年生活费需要多少？",
            "聊聊伦敦和其他城市的生活成本。",
            "留学生活"
        )

        Spacer(Modifier.height(12.dp))

        PostCard(
            "英国计算机专业如何选择院校？",
            "一起讨论课程设置和专业方向。",
            "院校专业"
        )
    }
}

// 社区页面
@Composable
fun CommunityPage() {

    val categories = listOf(
        "全部", "申请经验", "留学生活", "选校讨论", "就业交流"
    )

    val posts = remember {
        mutableStateListOf(
            CommunityPost(
                1, "英国留学申请时间线分享",
                "从准备材料到提交申请，分享我的申请规划和时间安排。",
                "申请经验", 12
            ),
            CommunityPost(
                2, "曼彻斯特大学的生活怎么样？",
                "想了解当地住宿、交通、生活成本和学习环境。",
                "留学生活", 8
            ),
            CommunityPost(
                3, "英国硕士选校求建议",
                "目前正在考虑几所英国大学，希望听听大家的建议。",
                "选校讨论", 15
            ),
            CommunityPost(
                4, "留学期间如何寻找实习？",
                "想了解英国求职和实习的相关经验。",
                "就业交流", 6
            )
        )
    }

    var selectedCategory by remember { mutableStateOf("全部") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<CommunityPost?>(null) }

    var likedPostIds by remember {
        mutableStateOf(setOf<Int>())
    }

    val filteredPosts = posts.filter {
        selectedCategory == "全部" ||
                it.category == selectedCategory
    }

    PageContainer {

        Text(
            "留学社区",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "与申请者和在读留学生交流经验",
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("＋ 发布帖子")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "帖子分类",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "社区动态 (${filteredPosts.size})",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        if (filteredPosts.isEmpty()) {
            Text(
                "该分类暂时没有帖子，来发布第一篇吧！",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        filteredPosts.forEach { post ->

            CommunityPostCard(
                post = post,
                liked = post.id in likedPostIds,
                onClick = {
                    selectedPost = post
                }
            )

            Spacer(Modifier.height(12.dp))
        }
    }

    // 发布帖子弹窗
    if (showCreateDialog) {
        CreatePostDialog(
            onDismiss = {
                showCreateDialog = false
            },
            onPublish = { title, content, category ->

                val newId = (posts.maxOfOrNull { it.id } ?: 0) + 1

                posts.add(
                    0,
                    CommunityPost(
                        id = newId,
                        title = title,
                        content = content,
                        category = category,
                        likes = 0
                    )
                )

                selectedCategory = "全部"
                showCreateDialog = false
            }
        )
    }

    // 帖子详情弹窗
    selectedPost?.let { post ->

        val liked = post.id in likedPostIds

        PostDetailDialog(
            post = post,
            liked = liked,
            onLike = {
                likedPostIds =
                    if (liked) {
                        likedPostIds - post.id
                    } else {
                        likedPostIds + post.id
                    }
            },
            onDismiss = {
                selectedPost = null
            }
        )
    }
}

// 帖子数据结构
data class CommunityPost(
    val id: Int,
    val title: String,
    val content: String,
    val category: String,
    val likes: Int
)

// 社区帖子卡片
@Composable
fun CommunityPostCard(
    post: CommunityPost,
    liked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                post.category,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                post.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                post.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "♥ ${post.likes + if (liked) 1 else 0}  ·  点击查看详情",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// 发布帖子弹窗
@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPublish: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("申请经验") }

    val categories = listOf(
        "申请经验", "留学生活", "选校讨论", "就业交流"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("发布新帖子")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("帖子标题") },
                    placeholder = { Text("请输入帖子标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("帖子内容") },
                    placeholder = { Text("分享你的留学经历或问题...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "选择分类",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(8.dp))

                categories.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() &&
                        content.isNotBlank(),
                onClick = {
                    onPublish(
                        title.trim(),
                        content.trim(),
                        category
                    )
                }
            ) {
                Text("发布")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 帖子详情弹窗

@Composable
fun PostDetailDialog(
    post: CommunityPost,
    liked: Boolean,
    onLike: () -> Unit,
    onDismiss: () -> Unit
) {
    // 当前帖子的评论
    val comments = remember(post.id) {
        mutableStateListOf(
            "这篇分享很有帮助，谢谢！",
            "想问一下申请材料需要提前多久准备？"
        )
    }

    var commentText by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(post.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // 帖子内容
                Text(
                    post.category,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    post.content,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(16.dp))

                // 点赞
                Text(
                    "点赞数：${post.likes + if (liked) 1 else 0}"
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(onClick = onLike) {
                    Text(
                        if (liked) "取消点赞" else "♥ 点赞"
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // 评论列表
                Text(
                    "全部评论（${comments.size}）",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                if (comments.isEmpty()) {
                    Text(
                        "还没有评论，来发表第一条评论吧！",
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    comments.forEachIndexed { index, comment ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    "用户 ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(comment)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 评论输入框
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("发表评论") },
                    placeholder = { Text("分享你的想法...") },
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val content = commentText.trim()

                        if (content.isNotEmpty()) {
                            comments.add(content)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("发送评论")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}


// 院校和专业数据模型：必须定义在顶层，供院校列表、专业列表和详情弹窗共同使用。
data class ProgramInfo(
    val name: String,
    val nameCn: String,
    val category: String,
    val degreeLevel: String = "待核验",
    val duration: String = "待核验",
    val tuition: String = "待核验",
    val ielts: String = "待核验",
    val academicRequirement: String = "待核验",
    val officialUrl: String = "待补充"
)

data class UniversityInfo(
    val name: String,
    val nameCn: String,
    val location: String,
    val description: String,
    val programs: List<ProgramInfo>
)

// 院校页面

@Composable
fun InstitutionPage() {
    var selectedSection by remember { mutableIntStateOf(0) } // 0=院校，1=专业
    var universityKeyword by remember { mutableStateOf("") }
    var programKeyword by remember { mutableStateOf("") }
    var selectedProgramCategory by remember { mutableStateOf("全部") }

    var selectedUniversity by remember { mutableStateOf<UniversityInfo?>(null) }
    var selectedProgram by remember { mutableStateOf<ProgramInfo?>(null) }

    val universities = remember {
        listOf(
            UniversityInfo(
                name = "University of Oxford",
                nameCn = "牛津大学",
                location = "英国 · 牛津",
                description = "英国知名研究型大学，提供多个学科领域的学习与研究机会。",
                programs = listOf(
                    ProgramInfo("MSc Computer Science", "计算机科学硕士", "计算机与信息技术"),
                    ProgramInfo("MSc Financial Economics", "金融经济学硕士", "经济与金融")
                )
            ),
            UniversityInfo(
                name = "University of Cambridge",
                nameCn = "剑桥大学",
                location = "英国 · 剑桥",
                description = "英国知名研究型大学，拥有多个学院和学术研究领域。",
                programs = listOf(
                    ProgramInfo("MPhil in Advanced Computer Science", "高级计算机科学硕士", "计算机与信息技术"),
                    ProgramInfo("MPhil in Management", "管理学硕士", "商业与管理")
                )
            ),
            UniversityInfo(
                name = "Imperial College London",
                nameCn = "帝国理工学院",
                location = "英国 · 伦敦",
                description = "以科学、工程、医学和商学等领域的教学与研究著称。",
                programs = listOf(
                    ProgramInfo("MSc Computing", "计算机硕士", "计算机与信息技术"),
                    ProgramInfo("MSc Finance", "金融学硕士", "经济与金融")
                )
            ),
            UniversityInfo(
                name = "University College London",
                nameCn = "伦敦大学学院",
                location = "英国 · 伦敦",
                description = "位于伦敦的综合性研究型大学，学科领域广泛。",
                programs = listOf(
                    ProgramInfo("MSc Computer Science", "计算机科学硕士", "计算机与信息技术"),
                    ProgramInfo("MSc Data Science", "数据科学硕士", "计算机与信息技术")
                )
            ),
            UniversityInfo(
                name = "The University of Manchester",
                nameCn = "曼彻斯特大学",
                location = "英国 · 曼彻斯特",
                description = "英国综合性研究型大学，设有多个学科和专业。",
                programs = listOf(
                    ProgramInfo("MSc Advanced Computer Science", "高级计算机科学硕士", "计算机与信息技术"),
                    ProgramInfo("MSc Management", "管理学硕士", "商业与管理")
                )
            ),
            UniversityInfo(
                name = "The University of Edinburgh",
                nameCn = "爱丁堡大学",
                location = "英国 · 爱丁堡",
                description = "位于苏格兰爱丁堡的综合性研究型大学。",
                programs = listOf(
                    ProgramInfo("MSc Artificial Intelligence", "人工智能硕士", "计算机与信息技术"),
                    ProgramInfo("MSc Business Analytics", "商业分析硕士", "商业与管理")
                )
            )
        )
    }

    val allPrograms = universities.flatMap { university ->
        university.programs.map { program -> university to program }
    }

    val programCategories = listOf("全部") +
            allPrograms.map { it.second.category }.distinct()

    val filteredUniversities = universities.filter { university ->
        university.name.contains(universityKeyword, ignoreCase = true) ||
                university.nameCn.contains(universityKeyword, ignoreCase = true)
    }

    val filteredPrograms = allPrograms.filter { (university, program) ->
        val categoryMatches =
            selectedProgramCategory == "全部" ||
                    program.category == selectedProgramCategory

        val keywordMatches =
            program.name.contains(programKeyword, ignoreCase = true) ||
                    program.nameCn.contains(programKeyword, ignoreCase = true) ||
                    university.name.contains(programKeyword, ignoreCase = true) ||
                    university.nameCn.contains(programKeyword, ignoreCase = true)

        categoryMatches && keywordMatches
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // 固定区域：标题
        Spacer(Modifier.height(16.dp))
        Text(
            text = "院校与专业",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "探索英国大学，找到适合自己的留学方向",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        // 固定区域：院校 / 专业切换
        TabRow(selectedTabIndex = selectedSection) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("院校") }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("专业") }
            )
        }

        Spacer(Modifier.height(12.dp))

        // 固定区域：搜索框
        OutlinedTextField(
            value = if (selectedSection == 0) universityKeyword else programKeyword,
            onValueChange = { value ->
                if (selectedSection == 0) {
                    universityKeyword = value
                } else {
                    programKeyword = value
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (selectedSection == 0) {
                        "搜索大学中文名或英文名"
                    } else {
                        "搜索专业名称或所属大学"
                    }
                )
            },
            singleLine = true
        )

        // 固定区域：专业分类，仅专业页显示
        if (selectedSection == 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                programCategories.forEach { category ->
                    FilterChip(
                        selected = selectedProgramCategory == category,
                        onClick = { selectedProgramCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 列表区域：只让下方列表滚动，顶部控件保持可见
        if (selectedSection == 0) {
            Text(
                text = "英国大学（${filteredUniversities.size}）",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            if (filteredUniversities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "没有找到相关院校",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredUniversities,
                        key = { university -> university.name }
                    ) { university ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUniversity = university },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    university.nameCn,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    university.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    university.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "查看院校详情和专业",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "专业列表（${filteredPrograms.size}）",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            if (filteredPrograms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "没有找到符合条件的专业",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredPrograms,
                        key = { item -> "${item.first.name}-${item.second.name}" }
                    ) { item ->
                        val university = item.first
                        val program = item.second

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProgram = program },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    program.nameCn,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    program.name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    university.nameCn,
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    program.category,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 同一时刻只显示一个弹窗，避免两个详情弹窗重叠
    when {
        selectedProgram != null -> {
            val program = selectedProgram!!
            AlertDialog(
                onDismissRequest = { selectedProgram = null },
                title = { Text(program.nameCn) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 450.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            program.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "专业基本信息",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        InfoRow("专业类别", program.category)
                        InfoRow("学位类型", program.degreeLevel)
                        InfoRow("学制", program.duration)

                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text(
                            "申请要求",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        InfoRow("国际学生学费", program.tuition)
                        InfoRow("IELTS 要求", program.ielts)
                        InfoRow("学术背景", program.academicRequirement)

                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text(
                            "官方信息",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoRow("官方课程页面", program.officialUrl)

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "提示：当前展示的是项目演示数据，具体招生要求、学费和学制需要以大学官方公布的信息为准。",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedProgram = null }) {
                        Text("关闭")
                    }
                }
            )
        }

        selectedUniversity != null -> {
            val university = selectedUniversity!!
            AlertDialog(
                onDismissRequest = { selectedUniversity = null },
                title = { Text(university.nameCn) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            university.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(university.location)
                        Spacer(Modifier.height(12.dp))
                        Text(university.description)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "相关专业",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))

                        university.programs.forEach { program ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clickable {
                                        selectedUniversity = null
                                        selectedProgram = program
                                    }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        program.nameCn,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        program.name,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "查看专业详情",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedUniversity = null }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

// 通用信息展示组件，放在顶层，供专业详情页面调用
@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 快捷入口卡片
@Composable
fun QuickCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// 帖子卡片
@Composable
fun PostCard(
    title: String,
    description: String,
    category: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                category,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(title, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(6.dp))

            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
