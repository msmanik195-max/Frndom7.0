sed -i 's/LazyColumn(/val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { reels.size })\n            androidx.compose.foundation.pager.VerticalPager(\n                state = pagerState,/g' app/src/main/java/com/example/ui/reels/ReelsScreen.kt

sed -i 's/items(reels, key = { it.id }) { reel ->/        { page ->\n                    val reel = reels[page]/g' app/src/main/java/com/example/ui/reels/ReelsScreen.kt

sed -i 's/.height(550.dp)/.fillMaxSize()/g' app/src/main/java/com/example/ui/reels/ReelsScreen.kt

sed -i 's/Brush.verticalGradient(/Color.Black\n\/* Brush.verticalGradient(/g' app/src/main/java/com/example/ui/reels/ReelsScreen.kt
sed -i 's/Color(0xFF020617)/Color(0xFF020617) *\//g' app/src/main/java/com/example/ui/reels/ReelsScreen.kt
