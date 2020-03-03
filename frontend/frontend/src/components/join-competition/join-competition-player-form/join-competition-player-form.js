import React from "react";
import "./join-competition-player-form.css";
import submitButtonImage from "./submitButton.png";
import ApiHelper from "../../../helpers/api-helper";

import {NotificationContainer, NotificationManager} from "react-notifications";
import TeamCollection from "./team-collection";
import DefaultTextInput from "../../common/default-text-input";

export class TextInputWithSubmitButton extends React.Component {
    constructor(props) {
        super(props);

        this.input = {};

        this.state = {
            text: ""
        }
    }

    onTextChanged = (text) => {
        this.setState(prevState => {
            const {onChange = (_val) => {}} = this.props;
            onChange(text);
            return {
                text: text
            }
        })
    };

    render() {
        const {type="text", placeholder="", containerStyle={}, buttonStyle={},
            inputStyle={}, imagePath="", onSubmit=(_value) => {}, alt="submit competition id",
            clearOnSubmit=false
        } = this.props;

        let addProps = {};

        if (this.props.submitOnKey !== undefined) {
            addProps.onKeyDown = event => {
                if (event.key.toLowerCase() === this.props.submitOnKey) {
                    event.preventDefault();
                    event.stopPropagation();
                    onSubmit(this.state.text);
                    if (clearOnSubmit) {
                        this.input.value = "";
                        this.input.focus();
                    }
                }
            }
        }

        return (
            <div className={"row"} style={{overflow: "hidden", ...containerStyle}}>
                <input placeholder={placeholder} type={type} style={{width: "100%", ...inputStyle}}
                       onChange={event => this.onTextChanged(event.target.value)} ref={el => this.input = el}
                       {...addProps}
                />
                <button style={buttonStyle} onClick={() => {
                    onSubmit(this.state.text);
                    if (clearOnSubmit) {
                        this.input.value = "";
                        this.input.focus();
                    }
                }} type={"submit"}><img src={imagePath} alt={alt}/></button>
            </div>
        )
    }
}

class JoinCompetitionPlayerForm extends React.Component {

    constructor(props) {
        super(props);

        this.gameId = {};
        this.state = {
            currentPage: "gamePinPage",
            items: []
        }
    }

    onGameIdSubmitButton = (gameId) => {
        console.log(gameId);
        this.gameId = gameId;
        const timeout = 1500;
        ApiHelper.checkPin({pin: gameId}).then(resp => {
            if (resp.status >= 300) {
                return {success: false}
            }
            return {success: true, json: resp.json()};
        }).then(obj => {
            if (obj.success) {
                obj.json.then(val => {
                    console.log({val});
                    if (val.exists) {
                        NotificationManager.success("Competition found successfully", "Success", timeout);
                        setTimeout(() => {
                            this.setState(prevState => {
                                return {currentPage: "enterTeamPage"};
                            })
                        }, timeout);
                    } else {
                        NotificationManager.error("No such competition or registration is closed", "Error", timeout);
                    }
                });
            } else {
                NotificationManager.error("Error happened", "Error", timeout);
            }
        })
    };

    setupTeamEventConnections() {
        if (this.eventSource === undefined) {
            this.eventSource = ApiHelper.teamCreationEventSource(this.gameId);
            this.eventSource.addEventListener("error",
                (err) => {
                    console.log("EventSource failed: ", err)
                });
            this.eventSource.addEventListener("message", (event) => {
                console.log({data: event.data});
                this.setState((prevState) => {
                    let arr = prevState.items.slice(0);
                    const elem = JSON.parse(event.data);
                    const index = arr.findIndex(el => {return el.teamName === elem.teamName});
                    if (index === -1) {
                        arr.push(elem);
                    } else {
                        arr[index] = elem;
                    }
                    
                    return {items: arr}
                });
            });
        }
    }

    closeTeamEventConnections() {
        if (this.eventSource !== undefined)
            this.eventSource.close();
    }

    componentWillUnmount() {
        this.closeTeamEventConnections();
    }

    gamePinPage() {
        const buttonStyle = {
            backgroundColor: "Transparent",
            padding: "-5px",
            border: "none",
            overflow: "hidden",
            marginLeft: "-50px"
        };
        const inputStyle = {
            border: "none",
            outline: "none",
            backgroundColor: "Transparent",
            fontSize: "16px",
            textAlign: "center",
        };
        const innerContainerStyle = {
            width: "20%",
            margin: "0 auto",
            borderRadius: "20px",
            paddingTop: "5px",
            paddingBottom: "5px",
            backgroundColor: "white"
        };

        return (
            <div style={{marginTop: "40px"}}>
                <div style={innerContainerStyle}>
                    <TextInputWithSubmitButton imagePath={submitButtonImage} containerStyle={{margin: "0 auto"}}
                                               placeholder={"ID игры"} onSubmit={this.onGameIdSubmitButton}
                                               buttonStyle={buttonStyle} inputStyle={inputStyle}
                    />
                </div>
            </div>
        )
    }

    enterTeamPage() {
        const items = this.state.items;
        return (
            <div style={{marginTop: "30px"}}>
                <div style={{marginTop: "10px", width: "50%", margin:"0 auto"}}>
                    <DefaultTextInput
                        placeholder={"Найти команду"}
                        style={{
                            width: "100%",
                            borderRadius: "20px",
                            paddingTop: "11px",
                            paddingBottom: "11px"
                          }}
                    />
                </div>
                <div style={{margin: "70px 15% 20px 15%",}}>
                <TeamCollection items={items} gamePin={this.gameId} onSubmit={this.onSubmit}
                />
                </div>
            </div>
        )
    }

    onSubmit = (teamName, password) => {
        console.log({teamName, password, props: this.gameId});
        const obj = {
            competitionPin: this.gameId,
            teamName: teamName,
            password: password
        };

        const timeout = 2200;

        ApiHelper.joinTeam(obj).then(resp => {
            if (resp.status >= 300) {
                return {success: false, json: resp.json()};
            } else {
                return {success: true, json: resp.json()};
            }
        }).then(resp => {
            resp.json.then(obj => {
                console.log(obj);
                if (resp.success) {
                    const teamName = obj.currentTeamName;
                    window.localStorage.setItem("currentTeamName", teamName);
                    NotificationManager.success("You joined team " + teamName, "Success", timeout)
                } else {
                    NotificationManager.error(obj.message, "Error", timeout);
                }
            })
        })
    };

    render() {
        let res;
        if (this.state.currentPage === "gamePinPage") {
            res = this.gamePinPage();
            this.closeTeamEventConnections();
        } else {
            res = this.enterTeamPage();
            this.setupTeamEventConnections()
        }
        return (
            <div>
                {res}
                <NotificationContainer/>
            </div>
        )
    }
}

export default JoinCompetitionPlayerForm;
