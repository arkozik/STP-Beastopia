package de.uniks.beastopia.teaml.controller;

import de.uniks.beastopia.teaml.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class PauseController extends Controller {

    @FXML
    private Button editProfileButton;
    @FXML
    private Button backToMainButton;
    @FXML
    private VBox friendListContainer;

    private final List<Controller> subControllers = new ArrayList<Controller>();
    @Inject
    App app;
    @Inject
    Provider<FriendListController> friendListControllerProvider;
    @Inject
    Provider<MenuController> menuControllerProvider;

    @Inject
    public PauseController() {

    }

    @Override
    public Parent render() {
        Parent parent = super.render();
        Controller subController = friendListControllerProvider.get();
        subControllers.add(subController);
        friendListContainer.getChildren().add(subController.render());
        return parent;
    }

    @Override
    public void destroy() {
        subControllers.forEach(Controller::destroy);
    }

    @Override
    public String getTitle() {
        return "Beastopia - Pause Menu";
    }

    public void editProfileButtonPressed(ActionEvent actionEvent) {
    }

    public void mainMenuButtonPressed(ActionEvent actionEvent) {
        app.show(menuControllerProvider.get());
    }
}