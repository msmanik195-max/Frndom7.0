cat app/src/main/java/com/example/ui/reels/ReelsScreen.kt | tr '\r' '\n' > temp_reels.kt
sed -i '1,5d' temp_reels.kt
echo -e "package com.example.ui.reels\nimport androidx.compose.foundation.layout.aspectRatio\n$(cat temp_reels.kt)" > app/src/main/java/com/example/ui/reels/ReelsScreen.kt
