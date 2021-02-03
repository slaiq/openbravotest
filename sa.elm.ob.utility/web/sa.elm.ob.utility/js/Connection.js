OB.EUTConnection = OB.EUTConnection || {};
OB.EUTConnection.OnChangeFunctions = OB.EUTConnection.OnChangeFunctions || {};

OB.EUTConnection.OnChangeFunctions.setURL = function (item, view, form, grid) {
  var url = 'jdbc:oracle:thin:@',
      hostname = form.getItem('hostname').getValue(),
      port = form.getItem('port').getValue(),
      sid = form.getItem('sid').getValue(),
      uname = form.getItem('username').getValue(),
      pwd = form.getItem('password').getValue();
  if (hostname && port && sid) {
    url = url + hostname + ':' + port + ":" + sid;
    form.getItem('connectionUrl').setValue(url);
  } else {
    form.getItem('connectionUrl').setValue(null);
  }
};