package test_assignment;

import test_assignment.controllers.UserController;
import test_assignment.controllers.UserRouter;
import test_assignment.jdbc.Database;
import test_assignment.repositories.IUserRepository;
import test_assignment.repositories.UserRepository;
import test_assignment.security.JwtSecurity;
import test_assignment.security.Security;
import test_assignment.utils.HttpUtils;
import test_assignment.utils.JwtUtils;

public class Main {
    public static void main(String[] args) {
        final JwtUtils jwtUtils = new JwtUtils();
        final HttpUtils httpUtils = new HttpUtils();
        final Database database = new Database();
        final Security jwtSecurity = new JwtSecurity(jwtUtils, httpUtils);
        final IUserRepository userRepository = new UserRepository(database);
        final UserController userController = new UserController(userRepository, jwtUtils, httpUtils);
        final UserRouter userRouter = new UserRouter(userController, jwtSecurity, httpUtils);

        final WebServer server = new WebServer();
        server.start(userRouter);
    }
}