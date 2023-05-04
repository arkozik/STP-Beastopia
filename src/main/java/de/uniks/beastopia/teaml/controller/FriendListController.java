package de.uniks.beastopia.teaml.controller;

import de.uniks.beastopia.teaml.model.User;
import de.uniks.beastopia.teaml.service.LoginService;
import de.uniks.beastopia.teaml.service.RefreshService;
import de.uniks.beastopia.teaml.service.TokenStorage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.List;

public class FriendListController extends Controller {
    @FXML
    public TextArea searchName;
    @FXML
    public Button searchBtn;
    @FXML
    public ScrollPane scrollFriends;
    @FXML
    public VBox friendList;
    @FXML
    public Button showChats;

    @Inject
    RefreshService refreshService;
    @Inject
    TokenStorage tokenStorage;

    @Inject
    public FriendListController() {

    }

    @Override
    public Parent render() {
        return super.render();
    }

    @Override
    public void init() {
        disposables.add(refreshService.refresh(tokenStorage.getRefreshToken()).subscribe(r -> {
            List<String> ids = r.getFriends();
            //für jede friend id user holen für ejden user subcxontroller
            User currentUser = new User()
                    .withFriends(r.getFriends());
            for (User friend : currentUser.getFriends()) {
                friendList.getChildren().add(new FriendController(friend).render());
            }
        }));
    }

    @FXML
    public void showChats(ActionEvent actionEvent) {

    }

    @FXML
    public void searchUser(ActionEvent actionEvent) {
    }
}
