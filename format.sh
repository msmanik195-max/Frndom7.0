cat app/src/main/java/com/example/ui/reels/ReelsScreen.kt | sed 's/import /\nimport /g' > temp.kt
cat temp.kt | sed 's/package /\npackage /g' > app/src/main/java/com/example/ui/reels/ReelsScreen.kt
