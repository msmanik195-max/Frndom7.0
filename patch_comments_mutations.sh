#!/bin/bash
sed -i 's/comments = comments + newComment/postRepository.addDetailedComment(newComment)/g' app/src/main/java/com/example/ui/components/CommentsBottomSheet.kt

sed -i 's/comments = comments.map {/postRepository.addDetailedComment(comment.copy(likesCount = if(comment.likesCount > 0) 0 else 1))/g' app/src/main/java/com/example/ui/components/CommentsBottomSheet.kt

sed -i '/if (it.id == comment.id) {/,/else it/d' app/src/main/java/com/example/ui/components/CommentsBottomSheet.kt
sed -i '/val newLikes/d' app/src/main/java/com/example/ui/components/CommentsBottomSheet.kt
