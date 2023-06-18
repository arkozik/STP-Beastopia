package de.uniks.beastopia.teaml.controller.ingame;

import de.uniks.beastopia.teaml.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.text.Text;

import javax.inject.Inject;

public class UserInfoController extends Controller {

    @FXML
    private Text nameText;
    @FXML
    private Text achievementsText;

    private String name;
    private int achievements;
    private int totalAchievements;

    @Inject
    public UserInfoController() {
    }

    public UserInfoController setAchievements(int achievements) {
        this.achievements = achievements;
        return this;
    }

    public UserInfoController setTotalAchievements(int totalAchievements) {
        this.totalAchievements = totalAchievements;
        return this;
    }

    public UserInfoController setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        nameText.setText(name);
        achievementsText.setText(achievements + "/" + totalAchievements + " Achievements");

        //TODO: load avatar
        return parent;
    }
}