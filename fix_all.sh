awk 'BEGIN { RS="import "; ORS="\nimport " } { if (NR==1) { gsub(/package com.example.ui.reels/, "package com.example.ui.reels\n"); print } else { print } }' app/src/main/java/com/example/ui/reels/ReelsScreen.kt > temp.kt
mv temp.kt app/src/main/java/com/example/ui/reels/ReelsScreen.kt
