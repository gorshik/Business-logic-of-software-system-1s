package ru.itmo.blss.firstlab.service;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.blss.firstlab.data.entity.Comment;
import ru.itmo.blss.firstlab.data.entity.Complain;
import ru.itmo.blss.firstlab.data.entity.User;
import ru.itmo.blss.firstlab.data.repository.ComplainRepository;

import javax.persistence.EntityNotFoundException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class ComplainsService {
    private final UserService userService;
    private final ComplainRepository complainRepository;
    private final CommentsService commentsService;
    private final ReportsService reportsService;
    private final UserTransaction userTransaction;

    public Complain getComplainById(int id) {
        return complainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    @Transactional
    public Complain newComplainForComment(int commentId, String payload, String login) throws SystemException {
        try {
            userTransaction.begin();
            Complain complain = new Complain();

            Comment comment = commentsService.getCommentById(commentId);

            if (comment.getAuthor().getLogin().equals(login)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя жаловаться на свой комментарий");
            }

            complain.setComment(comment);

            User author = userService.getByLogin(login);
            complain.setAuthor(author);

            complain.setPayload(payload);
            complain.setCreated(LocalDateTime.now());
            complain = complainRepository.save(complain);

            if (complainRepository.countComplainsByComment(comment) == 5) {
                reportsService.newReport(comment);
            }
            userTransaction.commit();
            return complain;
        } catch (Exception e) {
            userTransaction.rollback();
            throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "Транзакция не пошла");
        }
    }

    public void deleteCommentComplains(Comment comment) {
        Iterable<Complain> complains = complainRepository.getAllByComment(comment);
        complains.forEach(complainRepository::delete);
    }
}
