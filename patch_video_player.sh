sed -i '206,210c\
        if (reel.mediaUrl.isNotBlank()) {\
            Box(modifier = Modifier.fillMaxSize().aspectRatio(9f / 16f), contentAlignment = Alignment.Center) {\
                com.example.ui.components.FrndomVideoPlayer(\
                    videoUrl = reel.mediaUrl,\
                    modifier = Modifier.fillMaxSize()\
                )\
            }' app/src/main/java/com/example/ui/reels/ReelsScreen.kt
