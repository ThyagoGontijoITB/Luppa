package com.luppa.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDeletionRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public List<Integer> findEventIdsByCreator(Integer userId) {
        return entityManager.createQuery("""
                select e.id
                from Event e
                where e.creator.id = :userId
                """, Integer.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Integer> findEventAddressIdsByCreator(Integer userId) {
        return entityManager.createQuery("""
                select e.address.id
                from Event e
                where e.creator.id = :userId
                """, Integer.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Integer> findEventContactIdsByCreator(Integer userId) {
        return entityManager.createQuery("""
                select e.contact.id
                from Event e
                where e.creator.id = :userId
                """, Integer.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<String> findEventImagePathsByCreator(Integer userId) {
        return entityManager.createQuery("""
                select e.coverImagePath
                from Event e
                where e.creator.id = :userId
                  and e.coverImagePath is not null
                  and e.coverImagePath <> ''
                """, String.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public int deleteReportsForUserCascade(Integer userId, List<Integer> eventIds) {
        int deletedForRatings = entityManager.createQuery("""
                delete from Report report
                where report.rating.id in (
                    select rating.id
                    from Rating rating
                    where rating.user.id = :userId
                       or rating.event.creator.id = :userId
                )
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        int deletedForUser = entityManager.createQuery("""
                delete from Report report
                where report.reporter.id = :userId
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        if (eventIds.isEmpty()) {
            return deletedForRatings + deletedForUser;
        }

        int deletedForEvents = entityManager.createQuery("""
                delete from Report report
                where report.event.id in :eventIds
                """)
                .setParameter("eventIds", eventIds)
                .executeUpdate();

        return deletedForRatings + deletedForUser + deletedForEvents;
    }

    public int deleteSavedEventsForUserCascade(Integer userId, List<Integer> eventIds) {
        int deletedForUser = entityManager.createQuery("""
                delete from SavedEvent savedEvent
                where savedEvent.user.id = :userId
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        if (eventIds.isEmpty()) {
            return deletedForUser;
        }

        int deletedForEvents = entityManager.createQuery("""
                delete from SavedEvent savedEvent
                where savedEvent.event.id in :eventIds
                """)
                .setParameter("eventIds", eventIds)
                .executeUpdate();

        return deletedForUser + deletedForEvents;
    }

    public int deleteRatingsForUserCascade(Integer userId, List<Integer> eventIds) {
        int deletedForUser = entityManager.createQuery("""
                delete from Rating rating
                where rating.user.id = :userId
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        if (eventIds.isEmpty()) {
            return deletedForUser;
        }

        int deletedForEvents = entityManager.createQuery("""
                delete from Rating rating
                where rating.event.id in :eventIds
                """)
                .setParameter("eventIds", eventIds)
                .executeUpdate();

        return deletedForUser + deletedForEvents;
    }

    public int deleteEventSocialLinks(List<Integer> eventIds) {
        if (eventIds.isEmpty()) {
            return 0;
        }
        return entityManager.createQuery("""
                delete from EventSocialLink socialLink
                where socialLink.event.id in :eventIds
                """)
                .setParameter("eventIds", eventIds)
                .executeUpdate();
    }

    public int deleteUserOwnedRecords(Integer userId) {
        int deleted = 0;
        deleted += entityManager.createNativeQuery("delete from preferenciaComunicacaoUsuario where idUsuario = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        deleted += entityManager.createNativeQuery("delete from consentimentoMarketingUsuario where idUsuario = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        deleted += entityManager.createNativeQuery("delete from aceiteLegalUsuario where idUsuario = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        deleted += entityManager.createNativeQuery("delete from tokenRedefinicaoSenha where idUsuario = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        deleted += entityManager.createNativeQuery("delete from tokenVerificacaoEmail where idUsuario = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        return deleted;
    }

    public int deleteEventsByCreator(Integer userId) {
        return entityManager.createQuery("""
                delete from Event event
                where event.creator.id = :userId
                """)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int deleteUnreferencedEventContacts(List<Integer> contactIds) {
        if (contactIds.isEmpty()) {
            return 0;
        }
        return entityManager.createNativeQuery("""
                delete from contatoEvento
                where idContato in (:contactIds)
                  and not exists (
                      select 1
                      from evento
                      where evento.idContato = contatoEvento.idContato
                  )
                """)
                .setParameter("contactIds", contactIds)
                .executeUpdate();
    }

    public int deleteUnreferencedEventAddresses(List<Integer> addressIds) {
        if (addressIds.isEmpty()) {
            return 0;
        }
        return entityManager.createNativeQuery("""
                delete from endereco
                where idEndereco in (:addressIds)
                  and not exists (
                      select 1
                      from evento
                      where evento.idEndereco = endereco.idEndereco
                  )
                """)
                .setParameter("addressIds", addressIds)
                .executeUpdate();
    }
}
