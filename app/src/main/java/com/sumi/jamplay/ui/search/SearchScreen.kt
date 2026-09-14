package com.sumi.jamplay.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.LoadState
import coil.compose.AsyncImage
import com.sumi.jamplay.domain.model.Track
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.sumi.jamplay.R
import com.sumi.jamplay.ui.player.PlayingWave
import com.sumi.jamplay.ui.player.PlayerViewModel
import com.sumi.jamplay.ui.theme.JamPlayPurple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop

@Composable
fun SearchScreen(
    padding: PaddingValues,
    searchViewModel: SearchViewViewModel,
    playerViewModel: PlayerViewModel,
    onTrackClick: (Track, List<Track>) -> Unit
) {
    val tracks = searchViewModel.tracks.collectAsLazyPagingItems()
    val recentSearches by searchViewModel.recentSearches.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val focusStateFlow = remember { MutableStateFlow(false) }
    val text by searchViewModel.text.collectAsState()
    var tfValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(searchViewModel.text.value))
    }
    val isSearching by searchViewModel.isSearching.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면 라이프사이클 기반 클릭 제어
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> searchViewModel.enableClicks()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> searchViewModel.disableClicks()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(focusStateFlow) {
        if (isSearching && !focusStateFlow.value) {
            focusRequester.requestFocus()
        }
        focusStateFlow
            .drop(1) // 초기값 무시
            .collect {
                searchViewModel.setSearching(it)
            }
    }

    BackHandler(enabled = isSearching) {
        // 최근 검색어 화면이면 뒤로가기 시 검색 결과 화면으로 전환
        searchViewModel.setSearching(false)
        focusManager.clearFocus()
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            OutlinedTextField(
                value = tfValue,
                onValueChange = {
                    tfValue = it
                    searchViewModel.onTextChanged(it.text)
                },
                label = { Text(stringResource(R.string.search_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        focusStateFlow.value = focusState.isFocused
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        searchViewModel.search()
                        focusManager.clearFocus()
                        searchViewModel.setSearching(false)
                    }
                ),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                tfValue = TextFieldValue("", selection = TextRange("".length))
                                searchViewModel.onTextChanged("")
                                if (!isSearching) {
                                    focusRequester.requestFocus()
                                    searchViewModel.setSearching(true)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (isSearching) {
                // 최근 검색어 화면
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recentSearches) { keyword ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tfValue = TextFieldValue(keyword, selection = TextRange(keyword.length))
                                    searchViewModel.onTextChanged(keyword)
                                    searchViewModel.search()
                                    focusManager.clearFocus()
                                    searchViewModel.setSearching(false)
                                }
                                .padding(top = 16.dp, bottom = 16.dp, start = 4.dp, end = 4.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(keyword)
                        }
                    }
                }
            } else {
                // 검색 결과 화면
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (tracks.itemCount == 0) {
                        // 검색 결과 없음 표시
                        Text(
                            text = stringResource(R.string.no_search_results),
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp) // 아이템 사이 간격 4dp
                        ) {
                            items(tracks) { track ->
                                track?.let {
                                    TrackItem(
                                        track = it,
                                        isCurrentTrack = currentTrack?.id == track.id,
                                        isPlaying = isPlaying
                                    ) {
                                        if (!searchViewModel.acceptsClicks) return@TrackItem

                                        val currentTrackList = (0 until tracks.itemCount)
                                            .mapNotNull { index -> tracks.peek(index) }
                                        onTrackClick(it, currentTrackList)
                                    }
                                }
                            }
                        }
                    }

                    if (tracks.loadState.refresh is LoadState.Loading || tracks.loadState.append is LoadState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(track: Track?, isCurrentTrack: Boolean, isPlaying: Boolean,onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            AsyncImage(
                model = track?.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
            )

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                )
                // 재생중 표시
                PlayingWave(
                    isPlaying = isPlaying,
                    barCount = 5,
                    barWidth = 3.dp,
                    barMaxHeight = 24.dp,
                    barMinHeight = 6.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            val animatedColor by animateColorAsState(
                targetValue = if (isCurrentTrack) JamPlayPurple else Color.White,
                animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
            )

            Text(
                text = track?.name ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = animatedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isCurrentTrack) Modifier.shadow(8.dp, spotColor = JamPlayPurple.copy(alpha = 0.8f)) else Modifier
            )
            Text(
                text = track?.artistName ?: "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}