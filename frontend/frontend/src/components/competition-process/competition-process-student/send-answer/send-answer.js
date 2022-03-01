import React from "react";

import "./send-answer.css";
import submitButtonImage from "../../../join-competition/join-competition-player-form/submitButton.png"
import {withTranslation} from "react-i18next";
import {
    TextInputWithSubmitButton
} from "../../../join-competition/join-competition-player-form/text-input-with-submit-button/text-input-with-submit-button";

class SendAnswer extends React.Component {
    render() {
        const buttonStyle = {
            backgroundColor: "Transparent",
            padding: "-5px",
            marginTop: "5px",
            marginBottom: "5px",
            border: "none",
            overflow: "hidden",
            marginLeft: "-50px",
        };

        const imgStyle = {
            height: "35px",
            width: "35px",
        };

        const inputStyle = {
            backgroundColor: "#FFFFFF",
            fontSize: "16px",
            textAlign: "center",
            border: "none",
            outline: "none"
        };

        return (
            <div className={"row"}>
                <div className={"col-4"} style={{textAlign: 'right', paddingRight: "20px", paddingTop: "10px"}}>
                    {this.props.i18n.t("competition_process.student.send_answer.production_in_new_round")}
                </div>
                <div className={"col-5"} style={{paddingRight: "50px"}}>
                    <TextInputWithSubmitButton
                        containerStyle={{width: "100%", borderRadius: "25px"}}
                        imagePath={submitButtonImage}
                        buttonStyle={buttonStyle}
                        inputStyle={inputStyle}
                        clearOnSubmit={true}
                        onSubmit={this.props.onSubmit}
                        submitOnKey={'enter'}
                        imgStyle={imgStyle}
                    />
                </div>
            </div>
        )
    }
}

export default withTranslation('translation')(SendAnswer);
