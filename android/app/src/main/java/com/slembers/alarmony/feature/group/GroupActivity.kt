package com.slembers.alarmony.feature.group

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.slembers.alarmony.R
import com.slembers.alarmony.feature.common.NavItem
import com.slembers.alarmony.feature.common.ui.compose.GroupCard
import com.slembers.alarmony.feature.common.ui.compose.GroupInvite
import com.slembers.alarmony.feature.common.ui.compose.GroupSubjet
import com.slembers.alarmony.feature.common.ui.compose.GroupTitle
import com.slembers.alarmony.model.db.GroupModel
import com.slembers.alarmony.model.db.memberList

@ExperimentalMaterial3Api
@ExperimentalGlideComposeApi
class GroupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroupScreen()
        }
    }
}

@Preview
@Composable
@ExperimentalMaterial3Api
@ExperimentalGlideComposeApi
fun GroupScreen(
    navController : NavHostController = rememberNavController(),
    groupViewModel : GroupModel = viewModel()
) {

    val content = LocalContext.current
    var notification = "\"this is text : ${groupViewModel.title.value} " +
            "\nhour : ${groupViewModel.alarmTime.value?.hour}" +
            "\nminue : ${groupViewModel.alarmTime.value?.minute}" +
            "\nampm : ${groupViewModel.alarmTime.value?.is24hour}" +
            "\nvibration : ${groupViewModel.vibration}" +
            "\nsound : ${groupViewModel.sound}" +
            "\nvolumn : ${groupViewModel.volumn}"

    Scaffold(
        topBar = {
            GroupToolBar(
                title = NavItem.Group.title,
                navEvent = navController
            )
         },
        bottomBar = {
            GroupBottomBar(
                text = "저장",
                onClick = {
                    Toast.makeText(content, notification, Toast.LENGTH_SHORT).show()
                }
            )
        },
        content = { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavItem.Group.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable( route = NavItem.Group.route ) { GroupScreenMain(navController) }
                composable( route = NavItem.Sound.route ) { SoundScreen(navController) }
                composable( route = NavItem.GroupMember.route ) { InviteScreen(navController) }
            }

        }
    )

}

@Preview
@Composable
@ExperimentalMaterial3Api
@ExperimentalGlideComposeApi
fun GroupScreenMain(
    navController : NavHostController = rememberNavController(),
    groupViewModel : GroupModel = viewModel(),
) {


    val title by groupViewModel.title.observeAsState("")
    val timePickerState by groupViewModel.alarmTime.observeAsState(
        TimePickerState(0,0,false)
    )
    val isWeeks = remember{ mutableStateMapOf(
        "월" to true,
        "화" to true,
        "수" to true,
        "목" to true,
        "금" to true,
        "토" to true,
        "일" to true
    )}
    val weekList = remember{ mutableStateListOf(groupViewModel.week.value?.stream()?.toArray()) }
    val weeks = remember{ mutableStateListOf("월","화","수","목","금","토","일") }
    var soundStatus by remember { mutableStateOf(true) }
    val vibration by groupViewModel.vibration.observeAsState(true)
    val soundName by groupViewModel.sound.observeAsState("노래제목")
    val soundVolume by groupViewModel.volumn.observeAsState(7f)

    val scrollerState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(10.dp)
            .verticalScroll(scrollerState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GroupCard(
            title = { GroupTitle(title = "그룹제목") },
            content = { GroupSubjet(
                title = title,
                onChangeValue = { groupViewModel.onChangeTitle(it) })
            }
        )
        GroupCard(
            title = { GroupTitle(title = "알람시간") },
            content = {
                TimeInput(
                    state = timePickerState,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 10.dp,
                        bottom = 0.dp,
                        end = 0.dp)
                )
            }
        )
        GroupCard(
            title = { GroupTitle(title = "요일선택") },
            content = {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val boxSize = this.maxWidth / 8
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                top = 0.dp,
                                bottom = 0.dp,
                                end = 10.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(weeks) {
                            TextButton(
                                modifier = Modifier.size(boxSize),
                                onClick = {
                                    isWeeks[it] = !isWeeks.getValue(it)
                                    if(isWeeks.getValue(it)) {
                                        groupViewModel.addAlarmWeeks(weeks)
                                    }
                                    Log.d("click event","isCheck key : $it value : ${isWeeks[it]}")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = Color.Black,
                                    containerColor =
                                    if(isWeeks.getValue(it)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.background
                                    },
                                ),
                                content = {
                                    Text( text = it )
                                }
                            )
                        }
                    }
                }
            }
        )
        GroupCard(
            title = { GroupTitle(
                title = NavItem.GroupMember.title,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp)
                    )},
                onClick = { navController.navigate( NavItem.GroupMember.route )}
            )},
            content = { GroupInvite(
                profiles = memberList)}
        )
        GroupCard(
            title = { GroupTitle(
                title = NavItem.Sound.title,
                onClick = { navController.navigate( NavItem.Sound.route) },
                content = {
                    Row(
                        modifier = Modifier.height(50.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = soundName,
                            style = TextStyle(
                                color = Color.Gray,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal
                            )
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_forward),
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            )}
        )
        GroupCard(
            title = { GroupTitle(
                title = "타입",
                content = {
                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(this.maxHeight)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(
                                        if (soundStatus)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.background
                                    )
                                    .clickable {
                                        soundStatus = !soundStatus
                                    }
                            ) {
                                Image(
                                    modifier = Modifier.align(Alignment.Center),
                                    painter = painterResource(id = R.drawable.baseline_music_note_24) ,
                                    contentDescription = null)
                            }
                        }

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(this.maxHeight)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(
                                        if (vibration == true)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.background
                                    )
                                    .clickable {
                                        vibration?.let {
                                            groupViewModel.onChangeVibration(!it)
                                            Log.i("vibration", "vibration value : $it")
                                        }
                                    }
                            ) {
                                Image(
                                    modifier = Modifier.align(Alignment.Center),
                                    painter = painterResource(id = R.drawable.baseline_vibration_24) ,
                                    contentDescription = null)
                            }
                        }
                    }
                }
            )},
        )
        GroupCard(
            title = { GroupTitle(
                title = "볼륨",
                content = {
                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .padding(5.dp)
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        vibration?.let { vibration ->
                            Slider(
                                value = soundVolume,
                                onValueChange = { groupViewModel.onChangeVolumn(it) },
                                valueRange = 0f..15f,
                                enabled = vibration,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.background,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                            )
                        }
                    }
                }
            )}
        )
    }
}