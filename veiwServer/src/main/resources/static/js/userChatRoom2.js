    //기존 코드
    
    const chatRoomId = "[[${chatRoomId}]]";
    const token = localStorage.getItem("token");
    const MemberId = localStorage.getItem("MemberId");
    var receiver = "[[${hospitalId}]]"; 

    console.log("병원 id 출력 : " + receiver);
    console.log("챗룸 id 출력 : " + chatRoomId);
	

    //const isNewChat = !chatRoomId || chatRoomId === "null";
	
	//새로운 채팅인지 기존 채팅내역이 있는지 확인하는 코드
    if (receiver != null && receiver !== "null") {
        console.log("새로운 채팅입니다. 웹소켓을 바로 연결합니다.");
        connectWebSocket(receiver);
    } else {
    	console.log("새로운 채팅이 아닙니다. 기존 채팅 내역을 불러옵니다.");
        $(document).ready(function() {
            $.ajax({
                url: 'http://localhost:9001/chat/' + chatRoomId,
                type: 'GET',
                dataType: 'json',
                headers: {
                    'Authorization': token,
                    'MemberId': MemberId
                },
                success: function(response) {
//                     const receivedContainer = $('.received-container');
//                     const sentContainer = $('.sent-container');
//                     receivedContainer.empty();
//                     sentContainer.empty();
					const hospitalName = response[0].chatRoom.hospital.hospitalName;
                    $('#hospitalName').text(hospitalName);
					const chatBox = $('#chat-box');
					
                    // 메시지들을 날짜 순으로 정렬
                    response.sort((a, b) => {
		                const dateA = new Date(a.sendDate[0], a.sendDate[1] - 1, a.sendDate[2], a.sendDate[3], a.sendDate[4], a.sendDate[5], a.sendDate[6] / 1000);
		                const dateB = new Date(b.sendDate[0], b.sendDate[1] - 1, b.sendDate[2], b.sendDate[3], b.sendDate[4], b.sendDate[5], b.sendDate[6] / 1000);
		                return dateA - dateB; // 오래된 것이 위로 오도록 오름차순 정렬
		            });
					
                    let finalReceiver = null;
                    const chatRoomInfo = response[0]; 
                    console.log("chatroominfo : " + chatRoomInfo.sender.id + chatRoomInfo.receiver.id);
                    
                    if (MemberId == chatRoomInfo.receiver.id) {
                        finalReceiver = chatRoomInfo.sender.id;
                    } else {
                        finalReceiver = chatRoomInfo.receiver.id;
                    }
                    
                    
                    response.forEach(chat => {
                        if (chat.message === "채팅이 연결되었습니다.") {
                            return;
                        }
						
                                                
                        const messageBox = $('<div class="messageBox"></div>');
                        const metaContainer = $('<div class="meta-container"></div>');
                        const messageElement = $('<div class="text-message"></div>');
                        const timestampElement = $('<div class="timestamp"></div>');
                        const isReadElement = $('<div class="is-read"></div>');
                        
                        const receiveImage = $('<img src="/images/chat_cat2-removebg-preview.png" class="receiveImage">');

                        var sendDateArray = chat.sendDate;


                        var sendDate = sendDateArray ? new Date(
                            sendDateArray[0],       // Year
                            sendDateArray[1] - 1,   // Month (0-based)
                            sendDateArray[2],       // Day
                            sendDateArray[3],       // Hour
                            sendDateArray[4],       // Minute
                            sendDateArray[5]        // Second

                        ) : null;

                        if (sendDate) {
                            var year = sendDate.getFullYear();
                            var month = ('0' + (sendDate.getMonth() + 1)).slice(-2); // 월은 0 기반이므로 +1
                            var day = ('0' + sendDate.getDate()).slice(-2);
                            var hours = sendDate.getHours();
                            var minutes = ('0' + sendDate.getMinutes()).slice(-2);
                            var seconds = ('0' + sendDate.getSeconds()).slice(-2);

                            // AM 또는 PM 결정
                            var ampm = hours >= 12 ? 'PM' : 'AM';
                            hours = hours % 12;
                            hours = hours ? hours : 12; // 0시를 12시로 변경

                            // 날짜 문자열 포맷팅
                            var formattedDate = year + '-' + month + '-' + day + ' ' +
                                                ampm + ' ' +
                                                ('0' + hours).slice(-2) + ':' + minutes + ':' + seconds;
                        } else {
                            var formattedDate = ''; 
                        }

     
                        timestampElement.text(formattedDate);

 
                        if (chat.isRead === false) {
                            isReadElement.text("1");
                        }else{
                        	isReadElement.text(" ");
                        }
                        

                        metaContainer.append(timestampElement);
                        metaContainer.append(isReadElement);
                        
                        messageElement.text(chat.message);

                        //receiver = chat.receiver.id; // AJAX 응답에서 receiver를 설정합니다.

                        //console.log("receiver 출력: " + receiver);

                        if (chat.sender.id == MemberId || chat.sender.id === null) {
                            messageElement.addClass('sentMsg');
                            messageBox.addClass('sentBox');
                            messageBox.append(messageElement);
                            messageBox.append(metaContainer);
                            chatBox.append(messageBox);
                        } else if (receiver != MemberId) {
                            messageElement.addClass('receivedMsg');
                            messageBox.addClass('receivedBox');
                            messageBox.append(receiveImage);
                            messageBox.append(messageElement);
                            messageBox.append(metaContainer);
                            
                            chatBox.append(messageBox);
                        }
                    });

                    $('#chat-box').scrollTop($('#chat-box').prop("scrollHeight"));

                    // 웹소켓 연결을 여기서 설정합니다.
                    connectWebSocket(finalReceiver);
                },
                error: function(xhr, status, error) {
                    console.error('채팅내역 불러오기 에러발생', error);
                },
                complete: function(jqXHR, textStatus) {
                    responseCheck(jqXHR);
                } 
            });
        });
        
        $.ajax({
            url: 'http://localhost:9001/chatList',
            type: 'GET',
            headers: {
                'Authorization': token,
                'MemberId': MemberId
            },
            success: function(response) {
                console.log(response);
                var chatList = $("#chatList");
                chatList.empty();  // 기존 내용을 비웁니다.

                if (response.length > 0) {
                    // lastMessageSendDate를 기준으로 정렬 (최신순)
                    response.sort((a, b) => {
                        return new Date(b.lastMessageSendDate) - new Date(a.lastMessageSendDate);
                    });

                    for (var i = 0; i < response.length; i++) {
                        var chat = response[i];

                        // moment.js를 사용하여 날짜 형식을 변환합니다.
                        var formattedDate = moment(chat.lastMessageSendDate).format('YYYY-MM-DD A hh:mm');

                        var listItem = `
                            <div class="chatListItem list-group-item list-group-item-action">
                                <div class="d-flex w-100 justify-content-between">
                                    <span class="mb-1">🏥 ${chat.hospitalName}</span>
                                    <small class="text-muted">마지막 채팅시간 <br> ${formattedDate}</small>
                                </div>
                                <span class="mb-1" style="text-align: left; display: block;">${chat.lastMessage}</span>
                                <button class="btn btn-outline-primary" onclick="chatLink(${chat.chatRoomId})">채팅하기</button>
                            </div>
                        `;
                        chatList.append(listItem);
                    }
                }
            },
            error: function(xhr, status, error) {
                console.error('채팅목록 불러오기 에러발생', error);
            },
            complete: function(jqXHR, textStatus) {
                responseCheck(jqXHR);
            } 
        });
    }
    
    
    function chatLink(chatRoomId) {
  
        localStorage.setItem('refreshNeeded', 'true');
        window.location.href = '/user/chat/' + chatRoomId;
    }
    document.addEventListener('DOMContentLoaded', (event) => {
    
        if (localStorage.getItem('refreshNeeded') === 'true') {
            localStorage.removeItem('refreshNeeded');
            location.reload();
        }
    });
	
	
	//웹소켓 연결 함수
    function connectWebSocket(receiver) {
        const sender = localStorage.getItem('MemberId');
        const socket = new WebSocket("ws://localhost:9001/ws/chat");

        socket.onopen = function(event) {
            console.log("웹소켓 연결 성공!");
            socket.send(JSON.stringify({
                type: 'join',
                sender: sender,
                receiver: receiver,
                //message: "채팅이 연결되었습니다."
            }));
            console.log("웹소켓 연결 시 sender : " + sender);
            console.log("웹소켓 연결 시 receiver : "+ receiver);
        };

        socket.onmessage = function(event) {
            const chatBox = $('#chat-box');
            
            const message = JSON.parse(event.data);
            const messageBox = $('<div class="messageBox"></div>');
            const messageElement = $('<div class="text-message"></div>');
            const metaContainer = $('<div class="meta-container"></div>');
            const timestampElement = $('<div class="timestamp"></div>');
            const isReadElement = $('<div class="is-read"></div>');

            const sendDate = message.sendDate;
            const localDate = new Date(
                sendDate[0], sendDate[1] - 1, sendDate[2], sendDate[3], sendDate[4], sendDate[5], sendDate[6] / 1000
            );

            const timestamp = !isNaN(localDate) ? localDate.toLocaleString('ko-KR') : 'Invalid Date';
            timestampElement.text(timestamp);

            messageElement.text(message.message);
            metaContainer.append(timestampElement);

            if (message.isRead === false) {
                isReadElement.text("1");
                metaContainer.append(isReadElement);
            }

            if (message.sender.id == sender) {
                messageElement.addClass('sentMsg');
                messageBox.addClass('sentBox');
                messageBox.append(messageElement);
                messageBox.append(metaContainer);
                chatBox.append(messageBox);  // jQuery append 사용
            } else {
                messageElement.addClass('receivedMsg');
                messageBox.addClass('receivedBox');
                messageBox.append(messageElement);
                messageBox.append(metaContainer);
                chatBox.append(messageBox);  // jQuery append 사용
            }

            chatBox.scrollTop(chatBox.prop("scrollHeight"));
        };

        socket.onclose = function() {
            console.log("서버 연결 해제");
        };
        
        
        function enterChatRoom(chatRoomId, userId) {
            if (socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    type: 'read',
                    chatRoomId: chatRoomId,
                    MemberId: MemberId
                }));
            } else {
                console.error('WebSocket 연결이 아직 열려 있지 않습니다.');
            }
        }
        

        document.getElementById('chat-form').addEventListener('submit', function(event) {
            event.preventDefault();
            const message = document.getElementById('inputMsg').value;
            socket.send(JSON.stringify({
                type: 'message',
                sender: sender,
                receiver: receiver,
                message: message
            }));
            document.getElementById('inputMsg').value = '';
        });
    }
