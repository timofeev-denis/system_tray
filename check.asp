<%
On Error Resume Next
Result = ""
Response.Write( "CreateObject: " )
Set OraDBSession = CreateObject("OracleInProcServer.XOraSession")
If Err.Number <> 0 Then
	Response.Write( "������ " & Err.Number & ": " & Err.Description & ". " )
ElseIf NOT IsObject( OraDBSession ) Then
	Response.Write( "������: �� ������� ������� ������ OracleInProcServer.XOraSession. " )
Else
	Response.Write( "OK. " )
End If

Response.Write( "OpenDatabase: " )
Set OraDB = OraDBSession.OpenDatabase( Request("sDBName"), Request("DBUser") & "/" & Request("DBPass"), 0 )
If Err.Number <> 0 Then
	Response.Write( "������ " & Err.Number & ": " & Err.Description & ". " )
ElseIf NOT IsObject( OraDB ) Then
	Response.Write( "������: �� ������� ������� ��. " )
Else
	Response.Write( "OK. " )
End If

Response.Write( "CreateDynaset: " )
Set DS = OraDB.CreateDynaset( "SELECT sysdate FROM dual", 0 )
If Err.Number <> 0 Then
	Response.Write( "������ " & Err.Number & ": " & Err.Description & ". " )
ElseIf NOT IsObject( DS ) Then
	Response.Write( "������: �� ������� ��������� ������. " )
Else
	Response.Write( "OK. " )
End If

%>