package ru.itmo.blss.main.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.blss.data.dto.ReportDto;
import ru.itmo.blss.main.data.entity.Comment;
import ru.itmo.blss.main.data.entity.Complain;
import ru.itmo.blss.main.data.entity.User;
import ru.itmo.blss.main.data.repository.ComplainRepository;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class ComplainsService {
    private final UserService userService;
    private final ComplainRepository complainRepository;
    private final CommentsService commentsService;
    private final UserTransaction userTransaction;
    private final StatusService statusService;
    private final KafkaService kafkaService;

    public Complain getComplainById(int id) {
        return complainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    @Transactional
    public Complain newComplainForComment(int commentId, String payload, String login) {
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

        // Important part. Send to kafka!
        try {
            if (complainRepository.countComplainsByComment(comment) > 1) {
                ReportDto reportDto = new ReportDto();
                reportDto.setCommentId(comment.getId());
                reportDto.setStatusId(statusService.getSubmittedStatus().getId());
                kafkaService.send(reportDto);
            }
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return complain;
    }

    public void deleteCommentComplains(Comment comment) {
        Iterable<Complain> complains = complainRepository.getAllByComment(comment);
        complains.forEach(complainRepository::delete);
    }
}
