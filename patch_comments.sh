#!/bin/bash
sed -i 's/import androidx.compose.runtime.setValue/import androidx.compose.runtime.setValue\nimport androidx.compose.runtime.collectAsState/g' app/src/main/java/com/example/ui/components/CommentsBottomSheet.kt

sed -i 's/var comments by remember { mutableStateOf(listOf<CommentItem>()) }/val comments by postRepository.getCommentsFlow(postId).collectAsState(initial = emptyList())/g' app/src/main/java/com/example/ui/components/CommentsBottomSheet.kt
