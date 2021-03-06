package ru.itmo.blss.main.service;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.blss.main.data.entity.Comment;
import ru.itmo.blss.main.data.entity.Post;
import ru.itmo.blss.main.data.entity.User;
import ru.itmo.blss.main.data.repository.CommentRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@AllArgsConstructor
public class CommentsService {
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final PostsService postsService;

    public List<Comment> getPostComments(Post post) {
        return commentRepository.findCommentsByPost(post);
    }

    public List<Comment> getPostComments(int postId) {
        return commentRepository.findCommentsByPostId(postId);
    }

    public Comment getCommentById(int id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    public Comment newCommentForPost(String message, int postId, String authorLogin) {
        Comment comment = new Comment();

        User user = userService.getByLogin(authorLogin);
        comment.setAuthor(user);

        Post post = postsService.getPostById(postId);
        comment.setPost(post);

        comment.setPayload(message);
        return commentRepository.save(comment);
    }

    public void deleteComment(int id, String login) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        if (isCommentAuthor(login, comment)) {
            deleteComment(comment);
        } else  {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав");
        }
    }

    private boolean isCommentAuthor(String login, Comment comment) {
        return comment.getAuthor().getLogin().equals(login);
    }

    public void deleteComment(Comment comment) {
        comment.setDeleted(true);
        commentRepository.save(comment);
    }
}
