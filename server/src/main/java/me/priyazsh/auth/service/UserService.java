package me.priyazsh.auth.service;

import me.priyazsh.auth.entity.User;
import me.priyazsh.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateByGoogleSub(String googleSub, String email, String name, String picture) {
        String safeName = (name == null || name.isBlank()) ? email : name;

        Optional<User> existing = userRepository.findByGoogleSub(googleSub);
        if (existing.isPresent()) {
            User user = existing.get();
            boolean changed = false;
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                changed = true;
            }
            if (safeName != null && !safeName.equals(user.getName())) {
                user.setName(safeName);
                changed = true;
            }
            if (picture != null && !picture.equals(user.getImage())) {
                user.setImage(picture);
                changed = true;
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                changed = true;
            }
            return changed ? userRepository.save(user) : user;
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setGoogleSub(googleSub);
            if (safeName != null) user.setName(safeName);
            if (picture != null) user.setImage(picture);
            user.setEmailVerified(true);
            return userRepository.save(user);
        }

        User newUser = new User(email, safeName, picture, googleSub);
        newUser.setEmailVerified(true);
        return userRepository.save(newUser);
    }

    public Optional<User> findById(UUID id) {
        return id == null ? Optional.empty() : userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return email == null ? Optional.empty() : userRepository.findByEmail(email);
    }
}
