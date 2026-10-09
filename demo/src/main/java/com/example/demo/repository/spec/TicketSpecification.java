package com.example.demo.repository.spec;

import com.example.demo.domain.Priority;
import com.example.demo.domain.Status;
import com.example.demo.domain.Ticket;
import com.example.demo.dto.TicketSearchRequestDto;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class TicketSpecification {

    private TicketSpecification() {
    }

    private static Specification<Ticket> hasStatus(Status status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, criteria, builder) -> builder.equal(root.get("status"), status);
    }

    private static Specification<Ticket> hasPriority(Priority priority) {
        if (priority == null) {
            return Specification.unrestricted();
        }
        return (root, criteria, builder) -> builder.equal(root.get("priority"), priority);
    }

    private static Specification<Ticket> hasAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            return Specification.unrestricted();
        }
        return (root, criteria, builder) -> builder.equal(root.get("assignee").get("id"), assigneeId);
    }

    private static Specification<Ticket> dueBefore(Instant dueBefore) {
        if (dueBefore == null) {
            return Specification.unrestricted();
        }
        return (root, criteria, builder) -> builder.lessThanOrEqualTo(root.get("dueDate"), dueBefore);
    }

    private static Specification<Ticket> dueAfter(Instant dueAfter) {
        if (dueAfter == null) {
            return Specification.unrestricted();
        }
        return (root, criteria, builder) -> builder.greaterThanOrEqualTo(root.get("dueDate"), dueAfter);
    }

    private static Specification<Ticket> titleContains(String title) {
        if (title == null) {
            return Specification.unrestricted();
        }
        return (root, criteria, builder) ->
                builder.like(builder.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Ticket> orderByPriority(Sort.Direction direction) {
        return (root, query, builder) -> {
            Expression<Integer> priorityRank = builder.<Integer>selectCase()
                    .when(builder.equal(root.get("priority"), Priority.URGENT), 4)
                    .when(builder.equal(root.get("priority"), Priority.HIGH), 3)
                    .when(builder.equal(root.get("priority"), Priority.MEDIUM), 2)
                    .otherwise(1);

            query.orderBy(direction == Sort.Direction.DESC
                    ? builder.desc(priorityRank)
                    : builder.asc(priorityRank));

            return builder.conjunction();
        };
    }

    public static Specification<Ticket> dueForDigest(Instant threshold) {
        return (root, criteria, builder) -> {
            if (Long.class != criteria.getResultType() && long.class != criteria.getResultType()) {
                root.fetch("assignee", JoinType.LEFT);
            }

            return builder.and(
                    builder.isNotNull(root.get("dueDate")),
                    builder.lessThan(root.get("dueDate"), threshold),
                    builder.notEqual(root.get("status"), Status.DONE)
            );
        };
    }

    public static Specification<Ticket> build(TicketSearchRequestDto request) {
        if (request == null) {
            return Specification.unrestricted();
        }

        return Specification.allOf(
                hasStatus(request.status()),
                hasPriority(request.priority()),
                hasAssignee(request.assigneeId()),
                dueBefore(request.dueBefore()),
                dueAfter(request.dueAfter()),
                titleContains(request.titleContains())
        );
    }
}
