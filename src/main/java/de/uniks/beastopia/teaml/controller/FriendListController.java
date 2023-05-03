package de.uniks.beastopia.teaml.controller;

import de.uniks.beastopia.teaml.model.User;
import de.uniks.beastopia.teaml.service.LoginService;
import io.reactivex.rxjava3.core.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

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
    public HBox friendList;
    @FXML
    public Button showChats;

    @Inject LoginService loginService;

    @Inject
    public FriendListController() {

    }

    @Override
    public Parent render() {
        return super.render();
    }

    @Override
    public void init() {
        loginService.login("string", "stringst").subscribe(lr -> {
            System.out.println(lr.getFriends());
            //friend subcontroller
        });
    }

    @FXML
    public void showChats(ActionEvent actionEvent) {

    }

    @FXML
    public void searchUser(ActionEvent actionEvent) {
    }
}
