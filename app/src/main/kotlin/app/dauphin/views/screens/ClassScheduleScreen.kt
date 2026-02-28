package app.dauphin.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.dauphin.data.CourseRepository
import app.dauphin.models.CourseItem
import app.dauphin.models.CourseResponse
import app.dauphin.views.screens.day.CourseCardView
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassScheduleScreen() {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context) }
    var courseData by remember { mutableStateOf<CourseResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val pagerState = rememberPagerState(pageCount = { days.size })

    LaunchedEffect(Unit) {
        val data = repository.getCourseData()//如果你懂學校api的話就直接改這行
        courseData = data
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // This adds padding to avoid the status bar (battery, time, etc.)
    ) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            days.forEachIndexed { index, day ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val dayOfWeekValue = (pageIndex + 1).toString()
                val classesForDay = courseData?.stuelelist?.filter { it.week == dayOfWeekValue } ?: emptyList()

                DayScheduleList(classes = classesForDay, weekday = pageIndex + 1)
            }
        }
    }
}

@Composable
fun DayScheduleList(classes: List<CourseItem>, weekday: Int) {
    if (classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No classes scheduled", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(classes.sortedBy { it.sess1 }) { course ->
                val startAndEnd = getSessionTimes(course.sess1, course.sess2)
                CourseCardView(
                    courseName = course.ch_cos_name,
                    roomNumber = course.room,
                    teacherName = course.teach_name,
                    startTime = startAndEnd.first,
                    endTime = startAndEnd.second,
                    stdNo = course.cos_ele_seq,
                    weekday = weekday
                )
            }
        }
    }
}

/**
 * Maps TKU session codes to actual times.
 */
private fun getSessionTimes(sessStart: String, sessEnd: String): Pair<Date, Date> {
    val sessionToTime = mapOf(
        "01" to (8 to 10),
        "02" to (9 to 10),
        "03" to (10 to 10),
        "04" to (11 to 10),
        "05" to (13 to 10),
        "06" to (14 to 10),
        "07" to (15 to 10),
        "08" to (16 to 10),
        "09" to (17 to 10),
        "10" to (18 to 20),
        "11" to (19 to 15),
        "12" to (20 to 10),
        "13" to (21 to 0),
        "A"  to (18 to 10),
        "B"  to (19 to 10),
        "C"  to (20 to 10),
        "D"  to (21 to 10)
    )

    fun createDate(sessionCode: String, isEnd: Boolean): Date {
        val cleanCode = sessionCode.trim()
        val time = sessionToTime[cleanCode] ?: (8 to 0)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, time.first)
        cal.set(Calendar.MINUTE, time.second)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        if (isEnd) {
            cal.add(Calendar.MINUTE, 50) // Each session is 50 mins
        }
        return cal.time
    }

    return createDate(sessStart, false) to createDate(sessEnd, true)
}
