package controller;

import model.User;

public interface UserAwareController {
    void setUser(User user);
}
