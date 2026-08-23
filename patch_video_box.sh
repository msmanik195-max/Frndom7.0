sed -i '206,207c\
        if (reel.mediaUrl.isNotBlank()) {\
            Box(modifier = Modifier.fillMaxSize().aspectRatio(9f / 16f, matchHeightConstraintsFirst = true).align(Alignment.Center)) {' app/src/main/java/com/example/ui/reels/ReelsScreen.kt
