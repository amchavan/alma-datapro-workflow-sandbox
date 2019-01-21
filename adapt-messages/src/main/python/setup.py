from setuptools import setup

dependencies = [
    "python-jose",
    "pika",
    "lxml"
]

setup(name='drawsmb',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/messages', 'draws/resources'],
      install_requires=dependencies,
      include_package_data=True,
      zip_safe=False)
